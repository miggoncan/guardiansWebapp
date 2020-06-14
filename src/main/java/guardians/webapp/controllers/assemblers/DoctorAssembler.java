package guardians.webapp.controllers.assemblers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import guardians.webapp.controllers.DoctorsController;
import guardians.webapp.model.Doctor;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is responsible for mapping the REST api links to links of this
 * webapp
 * 
 * @author miggoncan
 */
@Component
@Slf4j
public class DoctorAssembler {
	@Value("${api.links.doctors}")
	private String doctorsLink;
	@Value("${api.links.doctorUpdate}")
	private String updateDoctorLink;
	@Value("${api.links.shiftconf}")
	private String shiftConfLink;

	/**
	 * This method will convert the links contained in the {@link EntityModel}
	 * representation of a {@link Doctor} (links pointing to the REST service), to
	 * links of this web application
	 * 
	 * @param entity
	 * @return The {@link Doctor} containing the links
	 */
	public Doctor toDoctor(EntityModel<Doctor> entity) {
		log.info("Request to map to doctor: " + entity);
		Doctor doctor = entity.getContent();
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
					methodToSelf = DoctorsController.class.getMethod("getDoctor", Long.class, Model.class);
				} catch (NoSuchMethodException e) {
					log.error("The method DoctorsController.getDoctor(Long, Model) does not exist");
				}
				Link linkToSelf = linkTo(methodToSelf, doctor.getId()).withSelfRel();
				log.debug("The link to self is: " + linkToSelf);
				links.put("self", linkToSelf.getHref());
			} else if (rel.equals(updateDoctorLink)) {
				log.debug("Parsing updateDoctor relation");
				Method methodToUpdate = null;
				try {
					methodToUpdate = DoctorsController.class.getMethod("editDoctor", Long.class, Model.class);
				} catch (NoSuchMethodException e) {
					log.error("The method DoctorsController.putDoctor(Doctor, Model) does not exist");
				}
				Link linkToUpdate = linkTo(methodToUpdate, doctor.getId()).withSelfRel();
				log.debug("The link to update is: " + linkToUpdate);
				links.put(updateDoctorLink, linkToUpdate.getHref());
			} else if (rel.equals(doctorsLink)) {
				log.debug("Parsing doctors relation: Pass");
			} else if (rel.equals(shiftConfLink)) {
				log.debug("Parsing shift-config relation: Pass");
			} else {
				log.warn("Unknown relation while parsing doctor's links: " + link.getRel().value());
			}
		}
		doctor.setLinks(links);
		log.info("The doctor's links are " + links);
		return doctor;
	}

	/**
	 * This method will convert from an iterable of {@link Doctor}
	 * {@link EntityModel}s to a list of {@link Doctor}s with links pointing to this
	 * web application
	 * 
	 * @param entities
	 * @return The created list of doctors
	 */
	public List<Doctor> toList(Iterable<EntityModel<Doctor>> entities) {
		List<Doctor> doctors = new LinkedList<>();
		for (EntityModel<Doctor> doctorEntity : entities) {
			doctors.add(this.toDoctor(doctorEntity));
		}
		return doctors;
	}
}
