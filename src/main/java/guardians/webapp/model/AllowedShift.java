package guardians.webapp.model;

import org.springframework.hateoas.server.core.Relation;

import lombok.Data;

/**
 * This class represents the information of an {@link AllowedShift}
 * 
 * @author miggoncan
 */
@Data
@Relation(value = "allowedShift", collectionRelation = "allowedShifts")
public class AllowedShift implements Comparable<AllowedShift>{
	private Integer id;
	private String shift;
	
	@Override
	public int compareTo(AllowedShift allowedShift) {
		if (allowedShift == null) {
			return -1;
		}
		
		int result = 0;
		Integer id = allowedShift.getId();
		if (id == null) {
			if (this.id == null) {
				result = 0;
			} else {
				result = -1;
			}
		} else if (this.id == null) {
			result = 1;
		} else {
			result = this.id - id;
		}

		return result;
	}
}
