package guardians.webapp.model;

import java.util.Set;

import org.springframework.hateoas.server.core.Relation;

import lombok.Data;

/**
 * This class contains the information related to the shifts assigned on a
 * certain day of a {@link Schedule}
 * 
 * @author miggoncan
 */
@Data
@Relation(value = "scheduleDay", collectionRelation = "scheduleDays")
public class ScheduleDay implements Comparable<ScheduleDay> {
	private Integer day;
	private Boolean isWorkingDay;
	private Set<Doctor> cycle;
	private Set<Doctor> shifts;
	private Set<Doctor> consultations;

	@Override
	public int compareTo(ScheduleDay scheduleDay) {
		if (scheduleDay == null) {
			return -1;
		}

		int result = 0;
		Integer scheduleDayDay = scheduleDay.getDay();
		if (scheduleDayDay == null) {
			if (this.day == null) {
				result = 0;
			} else {
				result = -1;
			}
		} else if (this.day == null) {
			result = 1;
		} else {
			result = this.day - scheduleDayDay;
		}

		return result;
	}
}
