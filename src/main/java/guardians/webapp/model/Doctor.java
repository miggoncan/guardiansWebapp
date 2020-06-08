package guardians.webapp.model;

import java.util.Map;

import org.springframework.hateoas.server.core.Relation;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

/**
 * This class represents the information related to a {@link Doctor}
 * 
 * @author miggoncan
 */
@Data
@Relation(value = "doctor", collectionRelation = "doctors")
public class Doctor {
	private Long id;
	private String firstName;
	private String lastNames;
	private String email;
	private String status;
	/**
	 * This attribute will not be serialized. Its main purpose is to provide the
	 * thymeleaf templates an easy way of evaluating the doctor's state.
	 * 
	 * For example, if this map contains an entry for the key "update", it means
	 * this doctor can be updated.
	 */
	@JsonIgnore
	private Map<String, String> links;
}
