package guardians.webapp.services;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.client.Hop;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException.BadRequest;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import guardians.webapp.model.Calendar;
import guardians.webapp.model.Schedule;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is responsible for retrieving and persisting information related
 * to {@link Calendar}s and {@link Schedule}s
 * 
 * @author miggoncan
 */
@Service
@Slf4j
public class ScheduleService extends MyService {
	@Value("${api.links.confirmSchedule}")
	private String confirmScheduleLink;
	
	/**
	 * This method will return a summary of all existent {@link Calendar}s in the
	 * REST service. A summarized {@link Calendar} contains the month, year and
	 * status of the associated {@link Schedule}
	 * 
	 * @return The resources retrieved from the REST service
	 */
	public CollectionModel<EntityModel<Schedule>> getSchedules() {
		log.info("Request to get the all schedules");
		ParameterizedTypeReference<CollectionModel<EntityModel<Schedule>>> schedulesTypeReference = 
				new ParameterizedTypeReference<CollectionModel<EntityModel<Schedule>>>() {};
		CollectionModel<EntityModel<Schedule>> scheduleResources = traverson
				.follow(schedulesLink)
				.toObject(schedulesTypeReference);
		log.info("The received resources are: " + scheduleResources);
		return scheduleResources;
	}

	/**
	 * This method will retrieve all the information related to a single
	 * {@link Schedule}.
	 * 
	 * @param yearMonth The year and month of the {@link Schedule} to be found
	 * @return The schedule if it exists, or null.
	 */
	public EntityModel<Schedule> getSchedule(YearMonth yearMonth) {
		log.info("Request to get schedule of: " + yearMonth);
		ParameterizedTypeReference<EntityModel<Schedule>> scheduleTypeReference = 
				new ParameterizedTypeReference<EntityModel<Schedule>>() {};
		EntityModel<Schedule> scheduleResource = traverson
				.follow(Hop.rel(scheduleLink).withParameter("yearMonth", yearMonth))
				.toObject(scheduleTypeReference);
		log.info("The received resource is: " + scheduleResource);
		return scheduleResource;
	}

	/**
	 * This method will request the generation of a {@link Schedule} provided a
	 * {@link Calendar}
	 * 
	 * @param calendar This calendar contains the configuration that will be used to
	 *                 generate the {@link Schedule}
	 * @return The generated {@link Schedule} resource.
	 */
	public EntityModel<Schedule> newSchedule(Calendar calendar) {
		log.info("Request to generate schedule of " + calendar.getYear() + "-" + calendar.getMonth());
		log.debug("The calendar is: " + calendar);

		YearMonth yearMonth = YearMonth.of(calendar.getYear(), calendar.getMonth());
		// This map will be used to expand the links to the resources
		Map<String, Object> paramsSchedule = new HashMap<>();
		paramsSchedule.put("yearMonth", yearMonth.toString());

		// Get the links required
		List<Link> links = this.getRootRequiredLinks(scheduleLink, calendarsLink, calendarLink);
		Link linkToSchedule = links.get(0).expand(paramsSchedule);
		log.debug("The link to the schedule is: " + linkToSchedule);
		Link linkToPostCalendar = links.get(1);
		log.debug("The link to POST the calendar is: " + linkToPostCalendar);
		Link linkToPutCalendar = links.get(2).expand(paramsSchedule);
		log.debug("The link to PUT the calendar is: " + linkToPutCalendar);

		RestTemplate restTemplate = restTemplateBuilder.build();
		EntityModel<Schedule> schedule = null;

		HttpHeaders headers = getSessionHeaders();

		HttpEntity<Calendar> req = new HttpEntity<>(headers);
		// First try to delete the schedule. This allows regenerating a schedule if
		// it has not already been confirmed
		log.info("Attempting to delete the schedule if it already exists");
		try {
			// TODO this may throw a Forbidden exception if the schedule is confirmed
			restTemplate.exchange(linkToSchedule.toUri(), HttpMethod.DELETE, req, Object.class);
			log.info("The schedule has been deleted");
		} catch (NotFound e) {
			log.info("The schedule did not exist");
		}

		// First, we will try to POST the calendar. If it fails, we will try to PUT it
		boolean calendarPersistedCorrectly = false;
		req = new HttpEntity<Calendar>(calendar, headers);
		ResponseEntity<Calendar> resp = null;
		try {
			log.info("Attemting to POST calendar");
			resp = restTemplate.exchange(linkToPostCalendar.toUri(), HttpMethod.POST, req, Calendar.class);
			log.debug("The response calendar is: " + resp);
			log.debug("The persisted calendar is:  " + resp.getBody());
			calendarPersistedCorrectly = true;
		} catch (BadRequest e) {
			log.info("Bad request: " + e);
			log.info("Attempting to PUT calendar");
			resp = restTemplate.exchange(linkToPutCalendar.toUri(), HttpMethod.PUT, req, Calendar.class);
			log.debug("The response calendar is: " + resp);
			log.debug("The persisted calendar is:  " + resp.getBody());
			calendarPersistedCorrectly = true;
		} catch (RestClientException e) {
			log.error("Unexpected exception occurred: " + e);
		}

		if (calendarPersistedCorrectly) {
			// Request to start generating schedule
			req = new HttpEntity<>(headers);
			restTemplate.exchange(linkToSchedule.getHref(), HttpMethod.POST, req, Object.class);
			// This type is used to decode the response schedule
			ParameterizedTypeReference<EntityModel<Schedule>> scheduleTypeReference = 
					new ParameterizedTypeReference<EntityModel<Schedule>>() {};
			// After the request is accepted, we have to wait for the schedule to generate
			try {
				do {
					log.info("Wating for the schedule to generate");
					Thread.sleep(500);
					log.info("Trying to request generated schedule");
					schedule = restTemplate.exchange(linkToSchedule.toUri(), HttpMethod.GET, req, scheduleTypeReference)
							.getBody();
					log.debug("The received schedule is: " + schedule);
				} while (schedule.getContent().getStatus().equals("BEING_GENERATED"));
			} catch (InterruptedException e) {
				log.error("Interrupted: " + e);
			}
		}

		return schedule;
	}

	public void confirmSchedule(YearMonth yearMonth) {
		log.info("Request to confirm schedule: " + yearMonth);
		EntityModel<Schedule> schedule = this.getSchedule(yearMonth);
		Optional<Link> linkToConfirmSchedule = schedule.getLink(confirmScheduleLink);
		if (!linkToConfirmSchedule.isPresent()) {
			log.warn("Trying to confirm a schedule that cannot be confirmed: " + yearMonth);
		} else {
			HttpEntity<Calendar> req = new HttpEntity<>(getSessionHeaders());
			RestTemplate restTemplate = restTemplateBuilder.build();
			log.info("Attempting to confirm the schedule");
			restTemplate.exchange(linkToConfirmSchedule.get().toUri(), HttpMethod.PUT, req, Object.class);
			log.info("If it existed, the schedule has been confirmed");
		}
	}
	
	public EntityModel<Schedule> updateSchedule(YearMonth yearMonth, Schedule schedule) {
		// TODO
		return null;
	}
}
