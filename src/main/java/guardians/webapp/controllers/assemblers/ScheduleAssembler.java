package guardians.webapp.controllers.assemblers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import guardians.webapp.controllers.ScheduleController;
import guardians.webapp.model.Schedule;
import guardians.webapp.model.ScheduleDay;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ScheduleAssembler {
	@Value("${api.links.calendar}")
	private String calendarLink;
	@Value("${api.links.schedules}")
	private String schedulesLink;
	@Value("${api.links.scheduleStatus")
	private String scheduleStatusLink;
	@Value("${api.links.confirmSchedule}")
	private String confirmScheduleLink;

	/**
	 * This method will convert the links contained in the {@link EntityModel}
	 * representation of a {@link Schedule} (links pointing to the REST service), to
	 * links of this web application
	 * 
	 * @param entity
	 * @return The {@link Schedule} containing the links
	 */
	public Schedule toSchedule(EntityModel<Schedule> entity) {
		log.info("Request to map to schedule: " + entity);

		log.info("Mapping links of the schedule");
		Schedule schedule = entity.getContent();
		YearMonth yearMonth = YearMonth.of(schedule.getYear(), schedule.getMonth());
		Map<String, String> links = new HashMap<>();
		for (Link link : entity.getLinks()) {
			String rel = link.getRel().value();
			if (rel.equals("self")) {
				log.debug("Parsing self relation");
				Method methodToSelf = null;
				try {
					// The methods can not be acquired through the convenient methodOn()
					// static method on WebMvcLinkBuilder because methodOn proxies the returned
					// value. However, the String class is defined as final, so it cannot be
					// extended (so it cannot be proxied)
					methodToSelf = ScheduleController.class.getMethod("getSchedule", YearMonth.class, 
							Boolean.class, Model.class);
				} catch (NoSuchMethodException e) {
					log.error("The method ScheduleController.getSchedule(YearMonth, Boolean, Model) does not exist");
				}
				Link linkToSelf = linkTo(methodToSelf, yearMonth).withSelfRel();
				log.debug("The link to self is: " + linkToSelf);
				links.put("self", linkToSelf.getHref());
			} else if (rel.equals(scheduleStatusLink)) {
				log.debug("Parsing schedule status relation: Pass");
			} else if (rel.equals(calendarLink)) {
				log.debug("Parsing calendar relation: Pass");
			} else if (rel.equals(confirmScheduleLink)) {
				log.debug("Parsing confirm schedule relation");
				Method methodToConfirm = null;
				try {
					methodToConfirm = ScheduleController.class.getMethod("confirmSchedule", YearMonth.class,
							Model.class);
				} catch (NoSuchMethodException e) {
					log.error("The method ScheduleController.confirmSchedule(YearMonth, Model) does not exist");
				}
				Link linkToConfirm = linkTo(methodToConfirm, yearMonth).withSelfRel();
				log.debug("The link to confirm is: " + linkToConfirm);
				links.put(confirmScheduleLink, linkToConfirm.getHref());
			} else {
				log.warn("Unknown relation while parsing schedule's links: " + link.getRel().value());
			}
		}
		log.info("The created links are: " + links);
		schedule.setLinks(links);

		SortedSet<ScheduleDay> scheduleDays = schedule.getDays();
		if (scheduleDays == null || scheduleDays.isEmpty()) {
			log.info("The schedule day does not contains days");
		} else {
			log.info("Mapping the schedule days to the weeks list");
			List<List<ScheduleDay>> weeks = new LinkedList<>();
			List<ScheduleDay> currWeek = new ArrayList<>(7);
			LocalDate dayOfMonth = yearMonth.atDay(1);
			// Fill the currWeek list until the dayOfMonth
			// E.g. if dayOfMonth is a Wednesday, two days have to be added (previous Monday
			// and Tuesday)
			int daysToAdd = dayOfMonth.getDayOfWeek().getValue() - 1;
			log.debug("The first day of the month has value: " + dayOfMonth.getDayOfWeek().getValue());
			log.debug("Number of days to be added before the first day of the month: " + daysToAdd);
			for (int i = 0; i < daysToAdd; i++) {
				int day = dayOfMonth.minusDays(daysToAdd - i).getDayOfMonth();
				currWeek.add(createEmtpyScheduleDay(day));
			}
			log.debug("The current week after adding the days before the first day of the month is: " + currWeek);
			// Now, fill the weeks list with the real schedule days
			for (ScheduleDay scheduleDay : schedule.getDays()) {
				if (scheduleDay.getShifts().isEmpty()) {
					scheduleDay.setShifts(Collections.emptySet());
				}
				if (scheduleDay.getConsultations().isEmpty()) {
					scheduleDay.setConsultations(Collections.emptySet());
				}
				currWeek.add(scheduleDay);

				if (dayOfMonth.getDayOfWeek() == DayOfWeek.SUNDAY) {
					log.debug("Adding week: " + currWeek);
					log.debug("The number of days being added is: " + currWeek.size());
					weeks.add(currWeek);
					currWeek = new ArrayList<>(7);
				}
				dayOfMonth = dayOfMonth.plusDays(1);
			}
			dayOfMonth = yearMonth.atEndOfMonth();
			// Now we add more days until reaching a Sunday
			daysToAdd = 7 - dayOfMonth.getDayOfWeek().getValue();
			log.debug("Number of days to be added after the last day of the month: " + daysToAdd);
			for (int i = 0; i < daysToAdd; i++) {
				int day = dayOfMonth.plusDays(i + 1).getDayOfMonth();
				currWeek.add(createEmtpyScheduleDay(day));
			}
			if (daysToAdd > 0) {
				log.debug("The current week after adding the days after the last day of the month is: " + currWeek);
				log.debug("The number of days being added is: " + currWeek.size());
				weeks.add(currWeek);
			}
			log.debug("The created list of weeks is: " + weeks);
			schedule.setWeeks(weeks);
		}

		log.debug("The mapped schedule is: " + schedule);
		return schedule;
	}

	private ScheduleDay createEmtpyScheduleDay(Integer day) {
		log.debug("Request to create an emtpy schedule day for: " + day);
		ScheduleDay scheduleDay = new ScheduleDay();
		scheduleDay.setDay(day);
		scheduleDay.setIsWorkingDay(false);
		scheduleDay.setCycle(null);
		scheduleDay.setShifts(null);
		scheduleDay.setConsultations(null);
		log.debug("The created schedule day is: " + scheduleDay);
		return scheduleDay;
	}

	/**
	 * This method will convert from an iterable of {@link Schedule}
	 * {@link EntityModel}s to a list of {@link Schedule}s with links pointing to
	 * this web application
	 * 
	 * @param entities
	 * @return The created list of doctors
	 */
	public List<Schedule> toList(Iterable<EntityModel<Schedule>> entities) {
		List<Schedule> schedules = new LinkedList<>();
		for (EntityModel<Schedule> scheduleEntity : entities) {
			schedules.add(this.toSchedule(scheduleEntity));
		}
		return schedules;
	}
}
