package guardians.webapp.controllers;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import guardians.webapp.controllers.assemblers.DoctorAssembler;
import guardians.webapp.controllers.assemblers.ScheduleAssembler;
import guardians.webapp.model.Calendar;
import guardians.webapp.model.DayConfiguration;
import guardians.webapp.model.Doctor;
import guardians.webapp.model.Schedule;
import guardians.webapp.services.DoctorService;
import guardians.webapp.services.Schedule2ExcelService;
import guardians.webapp.services.ScheduleService;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is reponsible for handling requests related to {@link Calendar}s
 * and their associated {@link Schedule}s
 * 
 * @author miggoncan
 */
@Controller
@RequestMapping("/schedules")
@Slf4j
public class ScheduleController {
	@Autowired
	private ScheduleService scheduleService;
	@Autowired
	private DoctorService doctorService;
	
	@Autowired
	private Schedule2ExcelService schedule2ExcelService; 
	
	@Autowired
	private ScheduleAssembler scheduleAssembler;
	@Autowired
	private DoctorAssembler doctorAssembler;
	
	@Value("${guardians.default.minShiftsPerDay}")
	private Integer defaultMinShiftsPerDay;
	@Value("${guardians.default.minConsultationsPerDay}")
	private Integer defaultMinConsultationsPerDay;
	
	@Value("${guardians.default.useXlsx}")
	private Boolean defaultUseXlsx;
	
	private static final String YEAR_MONTH_ATTR = "yearMonth";
	private static final String START_DATE_ATTR = "startDate";
	private static final String END_DATE_ATTR = "endDate";
	private static final String DOCTORS_ATTR = "doctors";
	private static final String DAY_CONFS_ATTR = "dayConfs";
	private static final String SCHEDULES_ATTR = "schedules";
	private static final String SCHEDULE_ATTR = "schedule";
	private static final String USE_LIST_ATTR = "useListView";
	
	private static final String SCHEDULE_HREF = "scheduleHref";
	
	/**
	 * This method handles requests to get a summarized list of the existent
	 * {@link Schedule}s in the application. This summarized list includes the month and
	 * year of the schedule, and their status.
	 * 
	 * @param model The model object that will be used to pass attributes to
	 *              thymeleaf
	 * @return The path to the thymeleaf template used to represent the schedules
	 *         list (The path is relative to resources/templates and does not
	 *         contain the file extension)
	 */
	@GetMapping("")
	public String getSchedulesSummary(Model model) {
		log.info("Request to get the schedule summaries");
		CollectionModel<EntityModel<Schedule>> scheduleEntities = scheduleService.getSchedules();
		List<Schedule> schedules = scheduleAssembler.toList(scheduleEntities);
		schedules.sort(Comparator.comparing(Schedule::getYear)
						.thenComparing(Schedule::getMonth)
						.reversed());
		model.addAttribute(SCHEDULES_ATTR, schedules);
		return "schedules/schedules";
	}
	
	@GetMapping("/{yearMonth}")
	public String getSchedule(@PathVariable YearMonth yearMonth, 
			@RequestParam(defaultValue = "false") Boolean useListView, Model model) {
		log.info("Request to get schedule: " + yearMonth);
		EntityModel<Schedule> scheduleEntity = scheduleService.getSchedule(yearMonth);
		log.debug("The received schedule is: " + scheduleEntity);
		model.addAttribute(SCHEDULE_ATTR, scheduleAssembler.toSchedule(scheduleEntity));
		model.addAttribute(YEAR_MONTH_ATTR, yearMonth.toString());
		model.addAttribute(USE_LIST_ATTR, useListView);
		return "schedules/schedule";
	}
	
	/**
	 * This method handles requests to get the form used to generate a new
	 * {@link Schedule}
	 * 
	 * @param yearMonth The year and month for which the schedule is to be generated
	 * @param model     The model object that will be used to pass attributes to
	 *                  thymeleaf
	 * @return The path to the thymeleaf template form to generate a {@link Schedule}
	 *         (The path is relative to resources/templates and does not contain the
	 *         file extension)
	 */
	@GetMapping("/new")
	public String newScheduleForm(@RequestParam YearMonth yearMonth, Model model) {
		log.info("Request to create the schedule for: " + yearMonth);
		
		model.addAttribute(YEAR_MONTH_ATTR, yearMonth.toString());
		model.addAttribute(START_DATE_ATTR, yearMonth.atDay(1).toString());
		model.addAttribute(END_DATE_ATTR, yearMonth.atEndOfMonth().toString());
		
		CollectionModel<EntityModel<Doctor>> doctorResources = doctorService.getAvailableDoctors();
		List<Doctor> doctors = this.doctorAssembler.toList(doctorResources);
		log.debug("Adding doctors: " + doctors);
		model.addAttribute(DOCTORS_ATTR, doctors);
		
		List<DayConfiguration> dayConfs = new LinkedList<>();
		DayConfiguration dayConf;
		LocalDate currDate = yearMonth.atDay(1);
		DayOfWeek currDayWeek = currDate.getDayOfWeek();
		while(currDate.getMonth().equals(yearMonth.getMonth())) {
			dayConf = new DayConfiguration();
			dayConf.setDay(currDate.getDayOfMonth());
			dayConf.setIsWorkingDay(currDayWeek != DayOfWeek.SATURDAY 
					&& currDayWeek != DayOfWeek.SUNDAY);
			dayConf.setNumShifts(defaultMinShiftsPerDay);
			dayConf.setNumConsultations(defaultMinConsultationsPerDay);
			
			dayConfs.add(dayConf);
			
			currDate = currDate.plusDays(1);
			currDayWeek = currDate.getDayOfWeek();
		}
		log.debug("Adding day configurations: " + dayConfs);
		model.addAttribute(DAY_CONFS_ATTR, dayConfs);
		
		return "schedules/newSchedule";
	}
	
	/**
	 * This method handles requests to generate a new {@link Schedule}
	 * 
	 * @param yearMonth The year and month for which the schedule is to be generated
	 * @return A Map that should be serialized into JSON. It will contain the key:
	 *         SCHEDULE_HREF whose value will be the URI where the schedule
	 *         information will be shown (A GET to this URI will return html
	 *         content).
	 */
	@PostMapping("/new")
	@ResponseBody
	public Map<String, String> newSchedule(@RequestParam YearMonth yearMonth, 
			@RequestBody List<DayConfiguration> dayConfs) {
		log.info("Request to create a schedule for " + yearMonth);
		log.debug("The day configurations are: " + dayConfs);
		Calendar calendar = new Calendar();
		calendar.setMonth(yearMonth.getMonthValue());
		calendar.setYear(yearMonth.getYear());
		calendar.setDayConfigurations(new TreeSet<>(dayConfs));
		EntityModel<Schedule> scheduleEntity = scheduleService.newSchedule(calendar);
		log.debug("The generated schedule is: " + scheduleEntity);
		Schedule schedule = scheduleAssembler.toSchedule(scheduleEntity);
		Map<String,String> response = new HashMap<>();
		response.put(SCHEDULE_HREF, schedule.getLinks().get("self"));
		return response;
	}
	
	@PostMapping("/{yearMonth}/confirm")
	public String confirmSchedule(@PathVariable YearMonth yearMonth, Model model) {
		log.info("Request to confirm schedule: " + yearMonth);
		scheduleService.confirmSchedule(yearMonth);
		return getSchedule(yearMonth, false, model);
	}
	
	@GetMapping(value = "/{yearMonth}/download-as-excel", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<byte[]> downloadExcelFor(@PathVariable YearMonth yearMonth, @RequestParam(required = false) Boolean useXlsx) {
		log.info("Request to get the excel for the schedule of " + yearMonth);
		if (useXlsx == null) {
			log.info("The filetype to be created was not specified. Using Xlsx: " + defaultUseXlsx);
			useXlsx = defaultUseXlsx;
		} else {
			log.info("The requested filetype is xlsx: " + useXlsx);
		}
		
		ResponseEntity<byte[]> resp = null;
		boolean scheduleFound = false;
		EntityModel<Schedule> scheduleEntity = scheduleService.getSchedule(yearMonth);
		if (scheduleEntity != null) {
			Schedule schedule = scheduleEntity.getContent();
			if (schedule != null) {
				scheduleFound = true;
				log.debug("Requesting conversion to excel");
				byte [] excelBytes = schedule2ExcelService.toExcel(schedule, useXlsx).toByteArray();
				String fileName = yearMonth.toString();
				fileName += useXlsx ? ".xlsx" : ".xls";
				log.debug("The filename of the file sent is: " + fileName);
				resp = ResponseEntity.ok()
						.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
						.body(excelBytes);
			}
		}
		
		if (!scheduleFound) {
			resp = ResponseEntity.notFound().build();
		}
		
		return resp;
	}
}
