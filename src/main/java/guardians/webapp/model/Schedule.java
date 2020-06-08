package guardians.webapp.model;

import java.util.SortedSet;

import org.springframework.hateoas.server.core.Relation;

import lombok.Data;

/**
 * This class contains the information related to the schedule of a certain
 * month
 * 
 * @author miggoncan
 */
@Data
@Relation(value = "schedule", collectionRelation = "schedules")
public class Schedule {
	private Integer month;
	private Integer year;
	private String status;
	private SortedSet<ScheduleDay> days;
}
