package guardians.webapp.model;

import java.util.List;
import java.util.SortedSet;

import org.springframework.hateoas.server.core.Relation;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

/**
 * This class contains the information related to the shift configuration of a
 * {@link Doctor}
 * 
 * @author miggoncan
 */
@Data
@Relation(value = "shiftConfig", collectionRelation = "shifConfigs")
public class ShiftConfiguration {
	private Long doctorId;
	private Integer maxShifts;
	private Integer minShifts;
	private Integer numConsultations;
	private Boolean doesCycleShifts;
	private Boolean hasShiftsOnlyWhenCycleShifts;
	private SortedSet<AllowedShift> unwantedShifts;
	private SortedSet<AllowedShift> wantedShifts;
	private SortedSet<AllowedShift> wantedConsultations;
	/**
	 * unwantedShiftsList, wantedShiftsList and wantedConsultationsList will be
	 * lists containing the "shifts" of the corresponding shift preference. E.g.
	 * ["Monday", "Thursday"]
	 * 
	 * They will not be serialized. They will be used in the edit-doctor template to
	 * check if this {@link ShiftConfiguration} contains a certain
	 * {@link AllowedShift}
	 */
	@JsonIgnore
	private List<String> unwantedShiftsList;
	@JsonIgnore
	private List<String> wantedShiftsList;
	@JsonIgnore
	private List<String> wantedConsultationsList;
}
