package guardians.webapp.model;

import java.util.Set;

import org.springframework.hateoas.server.core.Relation;

import lombok.Data;

/**
 * This class represents the information related to the configurtion of a
 * certain {@link Calendar} day
 * 
 * @author miggoncan
 */
@Data
@Relation(value = "dayConfiguration", collectionRelation = "dayConfigurations")
public class DayConfiguration implements Comparable<DayConfiguration> {
	private Integer day;
	private Boolean isWorkingDay;
	private Integer numShifts;
	private Integer numConsultations;
	private Set<Doctor> unwantedShifts;
	private Set<Doctor> wantedShifts;

	@Override
	public int compareTo(DayConfiguration dayConf) {
		if (dayConf == null) {
			return -1;
		}

		int result = 0;
		Integer dayConfday = dayConf.getDay();
		if (dayConfday == null) {
			if (this.day == null) {
				result = 0;
			} else {
				result = -1;
			}
		} else if (this.day == null) {
			result = 1;
		} else {
			result = this.day - dayConfday;
		}

		return result;
	}
}
