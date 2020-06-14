package guardians.webapp.services;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.client.Hop;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException.BadRequest;
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

		EntityModel<Schedule> schedule = null;
		YearMonth yearMonth = YearMonth.of(calendar.getYear(), calendar.getMonth());
		ParameterizedTypeReference<EntityModel<Schedule>> scheduleTypeReference = new ParameterizedTypeReference<EntityModel<Schedule>>() {
		};

		// First, we will try to POST the calendar. If it fails, we will try to PUT it
		Link linkToPostCalendar = this.getRootRequiredLink(calendarsLink);
		log.debug("The link to POST the calendar is: " + linkToPostCalendar);
		Map<String, Object> paramsSchedule = new HashMap<>();
		paramsSchedule.put("yearMonth", yearMonth.toString());
		Link linkToPostSchedule = this.getRootRequiredLink(scheduleLink).expand(paramsSchedule);
		log.debug("The link to POST the schedule is: " + linkToPostSchedule);

		boolean calendarPersistedCorrectly = false;

		RestTemplate restTemplate = restTemplateBuilder.build();
		HttpEntity<Calendar> req = new HttpEntity<Calendar>(calendar, this.getSessionHeaders());
		ResponseEntity<Calendar> resp = null;
		try {
			resp = restTemplate.exchange(linkToPostCalendar.toUri(), HttpMethod.POST, req, Calendar.class);
			log.debug("The response calendar is: " + resp);
			log.debug("The persisted calendar is:  " + resp.getBody());
			calendarPersistedCorrectly = true;
		} catch (BadRequest e) {
			log.info("Bad request: " + e);
			// TODO Put calendar
		} catch (RestClientException e) {
			log.error("Unexpected exception occured: " + e);
		}

		if (calendarPersistedCorrectly) {
			// Request to start generating schedule
			req = new HttpEntity<>(getSessionHeaders());
			restTemplate.exchange(linkToPostSchedule.getHref(), HttpMethod.POST, req, Object.class);
			// After the request is accepted, we have to wait for the schedule to generate
			try {
				do {
					log.info("Wating for the schedule to generate");
					Thread.sleep(500);
					log.info("Trying to request generated schedule");
					schedule = traverson.follow(Hop.rel(scheduleLink).withParameters(paramsSchedule))
							.toObject(scheduleTypeReference);
					log.debug("The received schedule is: " + schedule);
				} while (schedule.getContent().getStatus().equals("BEING_GENERATED"));
			} catch (InterruptedException e) {
				log.error("Interrupted: " + e);
			}
		}

		return schedule;
	}
}
