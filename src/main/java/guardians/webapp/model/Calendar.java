package guardians.webapp.model;

import java.util.SortedSet;

import org.springframework.hateoas.server.core.Relation;

import lombok.Data;

/**
 * This class represents the information related to a {@link Calendar}
 * 
 * @author miggoncan
 */
@Data
@Relation(value = "calendar", collectionRelation = "calendars")
public class Calendar {
	private Integer month;
	private Integer year;
	private SortedSet<DayConfiguration> dayConfigurations;
}
