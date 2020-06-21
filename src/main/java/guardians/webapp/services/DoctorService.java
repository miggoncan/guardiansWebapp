package guardians.webapp.services;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.client.Hop;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import guardians.webapp.model.Doctor;
import guardians.webapp.model.ShiftConfiguration;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is responsible for retrieving and persisting information
 * related to {@link Doctor} and its {@link ShiftConfiguration}
 * 
 * @author miggoncan
 */
@Service
@Slf4j
public class DoctorService extends MyService {
	
	/**
	 * @return All the {@link Doctor} resources available at the REST service
	 */
	public CollectionModel<EntityModel<Doctor>> getDoctors() {
		log.info("Request to get all doctor resources");
		ParameterizedTypeReference<CollectionModel<EntityModel<Doctor>>> doctorTypeReference = 
				new ParameterizedTypeReference<CollectionModel<EntityModel<Doctor>>>() {};
		CollectionModel<EntityModel<Doctor>> doctorResources = traverson
				.follow(doctorsLink)
				.toObject(doctorTypeReference);
		log.info("The received resources are: " + doctorResources);
		return doctorResources;
	}
	
	/**
	 * @return Only the {@link Doctor} resources not DELETED
	 */
	public CollectionModel<EntityModel<Doctor>> getAvailableDoctors() {
		log.info("Request to get all available doctor resources");
		CollectionModel<EntityModel<Doctor>> doctorResources = this.getDoctors();
		List<EntityModel<Doctor>> availableDoctorResources = doctorResources.getContent()
			.stream()
			.filter(doctorResource -> doctorResource.hasLink(updateDoctorLink))
			.collect(Collectors.toList());
		Links links = doctorResources.getLinks();
		log.info("The available doctors are: " + availableDoctorResources);
		return CollectionModel.of(availableDoctorResources, links);
	}

	/**
	 * Request a doctor resource to the REST service
	 * 
	 * @param doctorId The id of the doctor
	 * @return The doctor resource
	 * @throws NotFound if the doctor was not found
	 */
	public EntityModel<Doctor> getDoctor(Long doctorId) {
		log.info("Request to get doctor " + doctorId);
		ParameterizedTypeReference<EntityModel<Doctor>> doctorTypeReference = 
				new ParameterizedTypeReference<EntityModel<Doctor>>() {};
		EntityModel<Doctor> doctorEntity;
		try {
			doctorEntity = traverson
					.follow(Hop.rel(doctorLink).withParameter("doctorId", doctorId))
					.toObject(doctorTypeReference);
		} catch (NotFound e) {
			log.info("The doctor was not found");
			throw e;
		}
		log.info("The received resource is: " + doctorEntity);
		return doctorEntity;
	}

	/**
	 * Request a shift configuration to the REST service
	 * 
	 * @param doctorId The id of the associated doctor
	 * @return The shift configuration resource. Can be null if not found.
	 */
	public EntityModel<ShiftConfiguration> getShiftConfiguration(Long doctorId) {
		log.info("Request to get shift configuration of doctor " + doctorId);
		ParameterizedTypeReference<EntityModel<ShiftConfiguration>> shiftContTypeReference = 
				new ParameterizedTypeReference<EntityModel<ShiftConfiguration>>() {};
		EntityModel<ShiftConfiguration> shiftConfigEntity;
		try {
			shiftConfigEntity = traverson
					.follow(Hop.rel(shiftConfLink).withParameter("doctorId", doctorId))
					.toObject(shiftContTypeReference);
			log.info("The received resource is: " + shiftConfigEntity);
		} catch (NotFound e) {
			log.info("The shift config was not found");
			shiftConfigEntity = null;
		}
		log.info("The received shift configuration is: " + shiftConfigEntity);
		return shiftConfigEntity;
	}

	/**
	 * Send a {@link Doctor} to the REST service to persist it
	 * 
	 * @param doctor    The doctor to be persisted
	 * @param startDate The date the doctor will have their first cycle shift. Can
	 *                  be null if the doctor has already been created.
	 * @return The persisted doctor
	 */
	public Doctor saveDoctor(Doctor doctor, @Nullable LocalDate startDate) {
		log.info("Request to persist doctor: " + doctor);

		// First, we need to know the link and method to persist the doctor
		Link linkToSaveDoctor = null;
		HttpMethod methodToSaveDoctor = null;
		if (doctor.getId() == null) {
			// This map will contain the startDate parameter to create the link
			Map<String, LocalDate> params = new HashMap<>();
			params.put("startDate", startDate);
			linkToSaveDoctor = this.getRootRequiredLinks(newDoctorLink).get(0).expand(params);
			methodToSaveDoctor = HttpMethod.POST;
		} else {
			// This map will contain the doctorId parameter to create the link
			Map<String, Long> params = new HashMap<>();
			params.put("doctorId", doctor.getId());
			linkToSaveDoctor = this.getRootRequiredLinks(doctorLink).get(0).expand(params);
			methodToSaveDoctor = HttpMethod.PUT;
		}
		log.debug("The link to create a doctor is: " + linkToSaveDoctor);

		// Persist the doctor
		RestTemplate restTemplate = restTemplateBuilder.build();
		HttpEntity<Doctor> req = new HttpEntity<Doctor>(doctor, this.getSessionHeaders());
		ResponseEntity<Doctor> responseDoctor = restTemplate.exchange(linkToSaveDoctor.toUri(), 
				methodToSaveDoctor, req, Doctor.class);
		log.debug("The response doctor is: " + responseDoctor);
		Doctor persistedDoctor;
		if (responseDoctor.getStatusCode() != HttpStatus.OK) {
			log.error("Unexpected http response: " + responseDoctor + ". Returning null");
			persistedDoctor = null;
		} else {
			persistedDoctor = responseDoctor.getBody();
			log.debug("The persisted doctor is: " + persistedDoctor);
		}
		
		return persistedDoctor;
	}

	/**
	 * Send a {@link ShiftConfiguration} to the REST service to persist it
	 * 
	 * @param shiftConf The shift configuration to be persisted
	 */
	public void saveShiftConfiguration(ShiftConfiguration shiftConf) {
		log.info("Request to persist the shift configuration " + shiftConf);
		
		// Request all the links we may need
		List<Link> links = this.getRootRequiredLinks(shiftConfLink, shiftConfsLink);

		// First, we will try to PUT the shift configuration. If it does not already
		// exist, we will then POST it
		Map<String, Long> params = new HashMap<>();
		params.put("doctorId", shiftConf.getDoctorId());
		Link linkToPersistShiftConf = links.get(0).expand(params);
		log.debug("The link to PUT the shift configuration is: " + linkToPersistShiftConf);
		RestTemplate restTemplate = restTemplateBuilder.build();
		HttpEntity<ShiftConfiguration> req = new HttpEntity<ShiftConfiguration>(shiftConf, this.getSessionHeaders());
		ResponseEntity<ShiftConfiguration> respShiftConf = null;
		try {
			respShiftConf = restTemplate.exchange(linkToPersistShiftConf.toUri(), HttpMethod.PUT, req,
					ShiftConfiguration.class);
			log.debug("The response shift configuration is: " + respShiftConf);
			log.debug("The persisted shift configuration is:  " + respShiftConf.getBody());
		} catch (NotFound e) {
			log.info("The shift configuration does not already exist. Attempting to create it");
			linkToPersistShiftConf = links.get(1);
			log.debug("The link to POST the shift configuration is: " + linkToPersistShiftConf);
			try {
				respShiftConf = restTemplate.postForEntity(linkToPersistShiftConf.toUri(), req,
						ShiftConfiguration.class);
				log.debug("The response shift configuration is: " + respShiftConf);
				log.debug("The persisted shift configuration is:  " + respShiftConf.getBody());
			} catch (RestClientException e1) {
				log.error("Unexpected exception occurred: " + e1);
			}
		} catch (RestClientException e) {
			log.error("Unexpected exception occurred: " + e);
		}
	}
}
