package guardians.webapp.controllers;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import guardians.webapp.controllers.assemblers.DoctorAssembler;
import guardians.webapp.model.AllowedShift;
import guardians.webapp.model.Doctor;
import guardians.webapp.model.ShiftConfiguration;
import guardians.webapp.services.AllowedShiftService;
import guardians.webapp.services.DoctorService;
import lombok.extern.slf4j.Slf4j;

/**
 * This controller is responsible for handling requests related to
 * {@link Doctor}s and their {@link ShiftConfiguration}s
 * 
 * @author miggoncan
 */
@Controller
@RequestMapping("/doctors")
@Slf4j
public class DoctorsController {
	@Autowired
	private DoctorAssembler doctorAssembler;

	@Autowired
	private DoctorService doctorService;
	@Autowired
	private AllowedShiftService allowedShiftService;

	// Default values that will be used on new shift configurations
	@Value("${guardians.default.minShifts}")
	private Integer defaultMinShifts;
	@Value("${guardians.default.maxShifts}")
	private Integer defaultMaxShifts;
	@Value("${guardians.default.numConsultations}")
	private Integer defaultNumConsultations;
	@Value("${guardians.default.doesCycleShifts}")
	private Boolean defaultDoesCycleShifts;
	@Value("${guardians.default.hasShiftOnlyWhenCycleShifts}")
	private Boolean defaultHasShiftsOnlyWhenCycleShifts;

	// Names of the thymeleaf model attributes
	private static final String DOCTORS_ATTR = "doctors";
	private static final String DOCTOR_ATTR = "doctor";
	private static final String SHIFT_CONF_ATTR = "shiftConfig";
	private static final String ALLOWED_SHIFTS_ATTR = "allowedShifts";

	/**
	 * This method handles requests to get the list of all available doctors in the
	 * application
	 * 
	 * @param model The model object that will be used to pass attributes to
	 *              thymeleaf
	 * @return The path to the thymeleaf template used to represent the doctors list
	 *         (The path is relative to resources/templates and does not contain the
	 *         file extension)
	 */
	@GetMapping("")
	public String getDoctors(Model model) {
		log.info("Request received get doctors");
		CollectionModel<EntityModel<Doctor>> doctorResources = doctorService.getDoctors();
		List<Doctor> doctors = this.doctorAssembler.toList(doctorResources);
		doctors.sort(Comparator.comparing(Doctor::getLastNames)
						.thenComparing(Doctor::getFirstName));
		model.addAttribute(DOCTORS_ATTR, doctors);
		return "doctors/doctors";
	}

	/**
	 * This method will handle requests to get all the information related to a
	 * {@link Doctor}. This includes their {@link ShiftConfiguration}
	 * 
	 * @param doctorId The id of the {@link Doctor}
	 * @param model    The model object that will be used to pass attributes to
	 *                 thymeleaf
	 * @return The path to the thymeleaf template used to represent a doctor (The
	 *         path is relative to resources/templates and does not contain the file
	 *         extension)
	 */
	@GetMapping("/{doctorId}")
	public String getDoctor(@PathVariable Long doctorId, Model model) {
		log.info("Request received get doctor " + doctorId);
		EntityModel<Doctor> doctorEntity = doctorService.getDoctor(doctorId);
		model.addAttribute(DOCTOR_ATTR, doctorAssembler.toDoctor(doctorEntity));
		EntityModel<ShiftConfiguration> shiftConfigEntity = doctorService.getShiftConfiguration(doctorId);
		ShiftConfiguration shiftConfig = null;
		if (shiftConfigEntity == null) {
			log.info("The doctor does not have an associated shift configuration");
		} else {
			shiftConfig = shiftConfigEntity.getContent();
		}
		model.addAttribute(SHIFT_CONF_ATTR, shiftConfig);
		return "doctors/doctor";
	}

	// TODO add option to delete doctor

	/**
	 * This method will handle requests to create a new {@link Doctor}, along with
	 * its {@link ShiftConfiguration}
	 * 
	 * @param formData The keys of the map will be the input names of edit-doctor
	 *                 form. E.g. "firstName". As input names can be repeated, the
	 *                 values of the map will be lists of Strings. Each String
	 *                 represents one of the values selected for a certain key. E.g.
	 *                 "wantedShit" = ["1", "2"]
	 * @param model    The model object that will be used to pass attributes to
	 *                 thymeleaf
	 * @return The path to the thymeleaf template used to represent the doctors list
	 *         (The path is relative to resources/templates and does not contain the
	 *         file extension)
	 */
	@PostMapping(value = "", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String postDoctor(@RequestBody MultiValueMap<String, String> formData, Model model) {
		log.info("Request received: edit doctor with data: " + formData);
		// The check boxes parameters are only sent if they are checked
		Doctor doctor = new Doctor();
		ShiftConfiguration shiftConf = null;
		LocalDate startDate = null;

		try {
			if (formData.containsKey("id") && !"".equals(formData.getFirst("id"))) {
				log.debug("The doctor already exists, as the id was provided");
				doctor.setId(Long.parseLong(formData.getFirst("id")));
				log.debug("The doctor's id is: " + doctor.getId());
				doctor = doctorService.getDoctor(doctor.getId()).getContent();
				EntityModel<ShiftConfiguration> shiftConfEntity = doctorService.getShiftConfiguration(doctor.getId());
				if (shiftConfEntity == null) {
					log.info("The doctor does not have an associated shift configuration. Creating a default one");
					shiftConf = getDefaultShiftConfig();
				} else {
					shiftConf = shiftConfEntity.getContent();
				}
			} else {
				log.debug("The doctor doesn't already exist. Creating an empty shift configuration");
				shiftConf = new ShiftConfiguration();
				startDate = LocalDate.parse(formData.getFirst("startDate"));
				log.debug("The start date is: " + startDate);
			}

			// Basic doctor information
			doctor.setLastNames(formData.getFirst("lastNames"));
			log.debug("The last names of the doctor are: " + doctor.getLastNames());
			doctor.setFirstName(formData.getFirst("firstName"));
			log.debug("The first name of the doctor is: " + doctor.getFirstName());
			doctor.setEmail(formData.getFirst("email"));
			log.debug("The email of the doctor is: " + doctor.getEmail());

			// Cycle shifts configuration
			boolean doesCycleShifts = this.isChecked(formData.getFirst("doesCycleShifts"));
			log.debug("The doctor does cycle shifts: " + doesCycleShifts);
			shiftConf.setDoesCycleShifts(doesCycleShifts);

			// Shifts configuration
			boolean doesShifts = this.isChecked(formData.getFirst("doesShifts"));
			log.debug("The doctor does shifts: " + doesShifts);
			boolean hasShiftsOnlyWhenCycleShifts = this.isChecked(formData.getFirst("hasShiftsOnlyWhenCycleShifts"));
			log.debug("The doctor has shifts only when cycle shifts: " + hasShiftsOnlyWhenCycleShifts);
			shiftConf.setHasShiftsOnlyWhenCycleShifts(doesShifts && hasShiftsOnlyWhenCycleShifts);
			log.debug("ShiftConfiguration.hasShiftOnlyWhenCycleShifts is: " + shiftConf.getHasShiftsOnlyWhenCycleShifts());
			if (doesShifts && !hasShiftsOnlyWhenCycleShifts) {
				log.debug("The doctor does shifts. Trying to parse minShifts and maxShifts");
				shiftConf.setMinShifts(Integer.parseInt(formData.getFirst("minShifts")));
				log.debug("Min shifts is " + shiftConf.getMinShifts());
				shiftConf.setMaxShifts(Integer.parseInt(formData.getFirst("maxShifts")));
				log.debug("Max shifts is " + shiftConf.getMaxShifts());
			} else {
				log.debug("Setting min and max shift to 0");
				shiftConf.setMinShifts(0);
				shiftConf.setMaxShifts(0);
			}

			// Consultations configuration
			boolean doesConsultations = this.isChecked(formData.getFirst("doesConsultations"));
			if (!doesConsultations) {
				log.debug("The doctor does not do consultations");
				shiftConf.setNumConsultations(0);
			} else {
				log.debug("The doctor does consultations");
				shiftConf.setNumConsultations(Integer.parseInt(formData.getFirst("numConsultations")));
				log.debug("The doctor should have " + shiftConf.getNumConsultations() + " consultations");
			}

			// Shift preferences
			CollectionModel<EntityModel<AllowedShift>> allowedShifts = allowedShiftService.getAllowedShifts();
			// allowedShiftMap will allow to easily map between a shift id and an AllowedShift
			Map<Integer, AllowedShift> allowedShiftMap = allowedShifts.getContent().stream()
					.map(allowedShiftEntity -> allowedShiftEntity.getContent())
					.collect(Collectors.toMap(AllowedShift::getId, allowedShift -> allowedShift));
			// Start checking shift preferences
			List<String> wantedShiftsStrings = formData.get("wantedShifts");
			shiftConf.setWantedShifts(mapShiftPreferences(wantedShiftsStrings, allowedShiftMap));
			log.debug("The wanted shifts are: " + shiftConf.getWantedShifts());
			List<String> unwantedShiftsStrings = formData.get("unwantedShifts");
			shiftConf.setUnwantedShifts(mapShiftPreferences(unwantedShiftsStrings, allowedShiftMap));
			log.debug("The unwanted shifts are: " + shiftConf.getUnwantedShifts());
			List<String> wantedConsultationsStrings = formData.get("wantedConsultations");
			shiftConf.setWantedConsultations(mapShiftPreferences(wantedConsultationsStrings, allowedShiftMap));
			log.debug("The wanted consultations are: " + shiftConf.getWantedConsultations());

			log.debug("The resulting doctor is: " + doctor);
			log.debug("The resulting shift configuration is: " + shiftConf);

			doctor = doctorService.saveDoctor(doctor, startDate);
			shiftConf.setDoctorId(doctor.getId());
			doctorService.saveShiftConfiguration(shiftConf);
		} catch (NumberFormatException e) {
			log.error(e.toString());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		} catch (DateTimeParseException e) {
			log.error(e.toString());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}

		return getDoctor(doctor.getId(), model);
	}
	
	/**
	 * @param checkBoxStr The String value from the MultiValueMap corresponding to
	 *                    the check box field.
	 * @return true if the check box has been checked.
	 */
	private boolean isChecked(@Nullable String checkBoxStr) {
		log.debug("Request to verify is " + checkBoxStr + " is checked");
		boolean isChecked = checkBoxStr != null && !"".equals(checkBoxStr)
				? "on".equals(checkBoxStr) 
				: false;
		log.debug("Is checked: " + isChecked);
		return isChecked;
	}

	/**
	 * This method converts a list of allowed shifts ids into a set of
	 * {@link AllowedShift}s
	 * 
	 * @param shiftIds        The list of ids. If it is null, the return set will be
	 *                        empty
	 * @param allowedShiftMap Used to map from an allowed shift id to an
	 *                        {@link AllowedShift}
	 * @return The mapped set of {@link AllowedShift}s
	 */
	private SortedSet<AllowedShift> mapShiftPreferences(@Nullable List<String> shiftIds, 
			Map<Integer, AllowedShift> allowedShiftMap) {
		log.debug("Request to map shift preferences: " + shiftIds);
		log.debug("The map of allowed shifts is: " + allowedShiftMap);
		if (shiftIds == null) {
			log.debug("shiftsIds is null. Returning an empty set.");
			return new TreeSet<>();
		}
		SortedSet<AllowedShift> shiftPreferences = shiftIds.stream()
				.map(idStr -> allowedShiftMap.get(Integer.parseInt(idStr)))
				.collect(Collectors.toCollection(() -> new TreeSet<>()));
		log.debug("The checked shift preferences are: " + shiftPreferences);
		return shiftPreferences;
	}

	/**
	 * This method handles requests to get the form used to edit a doctor
	 * 
	 * @param doctorId The id of the doctor that is to be edited. To create a new
	 *                 doctor, this parameter should be null.
	 * @param model    The model object that will be used to pass attributes to
	 *                 thymeleaf
	 * @return The path to the thymeleaf template containing the form to edit
	 *         doctor's information (The path is relative to resources/templates and
	 *         does not contain the file extension)
	 */
	@GetMapping({ "/edit/{doctorId}", "/edit" })
	public String editDoctor(@PathVariable(required = false) Long doctorId, Model model) {
		log.info("Request received: get form to edit doctor with id " + doctorId);
		Doctor doctor = null;
		ShiftConfiguration shiftConfig = getDefaultShiftConfig();

		if (doctorId != null) {
			// Maybe, these two requests could be performed simultaneously (Asynchronous)
			EntityModel<Doctor> doctorResource = doctorService.getDoctor(doctorId);
			if (doctorResource != null) {
				doctor = doctorAssembler.toDoctor(doctorResource);
			}

			EntityModel<ShiftConfiguration> shiftConfigResource = doctorService.getShiftConfiguration(doctorId);
			if (shiftConfigResource != null) {
				shiftConfig = shiftConfigResource.getContent();
			}
		}
		// The allowed shifts will be represented as a list
		CollectionModel<EntityModel<AllowedShift>> allowedShiftResources = allowedShiftService.getAllowedShifts();
		List<AllowedShift> allowedShifts = new LinkedList<>();
		for (EntityModel<AllowedShift> allowedShiftResource : allowedShiftResources) {
			allowedShifts.add(allowedShiftResource.getContent());
		}
		allowedShifts.sort(Comparator.comparingInt(AllowedShift::getId));
		log.debug("The allowed shifts after being sorted are: " + allowedShifts);

		// Map the shift preferences to lists. This will make it easier to check if a
		// shift preference contains a certain shift
		Set<AllowedShift> shiftPreferences = shiftConfig.getUnwantedShifts();
		List<String> shiftPreferencesList = new LinkedList<>();
		if (shiftPreferences != null) {
			shiftPreferencesList = shiftPreferences.stream().map(shift -> shift.getShift())
					.collect(Collectors.toCollection(() -> new LinkedList<String>()));
		}
		shiftConfig.setUnwantedShiftsList(shiftPreferencesList);
		shiftPreferences = shiftConfig.getWantedShifts();
		shiftPreferencesList = new LinkedList<>();
		if (shiftPreferences != null) {
			shiftPreferencesList = shiftPreferences.stream().map(shift -> shift.getShift())
					.collect(Collectors.toCollection(() -> new LinkedList<String>()));
		}
		shiftConfig.setWantedShiftsList(shiftPreferencesList);
		shiftPreferences = shiftConfig.getWantedConsultations();
		shiftPreferencesList = new LinkedList<>();
		if (shiftPreferences != null) {
			shiftPreferencesList = shiftPreferences.stream().map(shift -> shift.getShift())
					.collect(Collectors.toCollection(() -> new LinkedList<String>()));
		}
		shiftConfig.setWantedConsultationsList(shiftPreferencesList);

		model.addAttribute(DOCTOR_ATTR, doctor);
		model.addAttribute(SHIFT_CONF_ATTR, shiftConfig);
		model.addAttribute(ALLOWED_SHIFTS_ATTR, allowedShifts);

		return "doctors/edit-doctor";
	}

	/**
	 * This method will create the default {@link ShiftConfiguration} of a
	 * {@link Doctor}
	 * 
	 * @return The default shift configuration of a doctor
	 */
	private ShiftConfiguration getDefaultShiftConfig() {
		log.info("Request to create a default shift configuration");
		ShiftConfiguration shiftConfig = new ShiftConfiguration();
		shiftConfig.setMinShifts(defaultMinShifts);
		shiftConfig.setMaxShifts(defaultMaxShifts);
		shiftConfig.setNumConsultations(defaultNumConsultations);
		shiftConfig.setDoesCycleShifts(defaultDoesCycleShifts);
		shiftConfig.setHasShiftsOnlyWhenCycleShifts(defaultHasShiftsOnlyWhenCycleShifts);
		log.debug("The created default configuration is: " + shiftConfig);
		return shiftConfig;
	}
}
