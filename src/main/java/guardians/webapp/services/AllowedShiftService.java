package guardians.webapp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.client.Traverson;
import org.springframework.stereotype.Service;

import guardians.webapp.model.AllowedShift;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is responsible for retrieving from the REST service information
 * related to {@link AllowedShift}s
 * 
 * @author miggoncan
 */
@Service
@Slf4j
public class AllowedShiftService {
	@Autowired
	private Traverson traverson;
	
	@Value("${api.links.allowedshifts}")
	private String allowedShiftsLink;

	/**
	 * This method will retrieve all available {@link AllowedShift} resources from
	 * the REST service
	 * 
	 * @return The collection of allowed shift resources
	 */
	public CollectionModel<EntityModel<AllowedShift>> getAllowedShifts() {
		log.info("Request to get all allowed shift resources");
		ParameterizedTypeReference<CollectionModel<EntityModel<AllowedShift>>> allowedShiftsTypeReference = 
				new ParameterizedTypeReference<CollectionModel<EntityModel<AllowedShift>>>() {};
		CollectionModel<EntityModel<AllowedShift>> allowedShiftResources = traverson.follow(allowedShiftsLink)
				.toObject(allowedShiftsTypeReference);
		log.info("The received resources are: " + allowedShiftResources);
		return allowedShiftResources;
	}
}
