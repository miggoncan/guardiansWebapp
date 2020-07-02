package guardians.webapp.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.util.LinkedList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import guardians.webapp.model.Doctor;
import guardians.webapp.model.Schedule;
import guardians.webapp.model.ScheduleDay;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Schedule2ExcelService {
	
	@Value("${guardians.excel.cyclic-shift.font.name}")
	private String csFontName;
	@Value("${guardians.excel.cyclic-shift.font.size}")
	private short csFontSize;
	@Value("${guardians.excel.cyclic-shift.font.isBold}")
	private Boolean csFontIsBold;
	
	@Value("${guardians.excel.shift.font.name}")
	private String sFontName;
	@Value("${guardians.excel.shift.font.size}")
	private short sFontSize;
	@Value("${guardians.excel.shift.font.isBold}")
	private Boolean sFontIsBold;
	
	@Value("${guardians.excel.first-column-width}")
	private Integer firstColWidth;
	@Value("${guardians.excel.column-width}")
	private Integer colWidth;

	public ByteArrayOutputStream toExcel(Schedule schedule, boolean useXlsx) throws IOException {
		log.info("Request to convert to excel");
		YearMonth yearMonth = YearMonth.of(schedule.getYear(), schedule.getMonth());
		log.info("The schedule is for " + yearMonth);
		Workbook workbook = useXlsx ? new XSSFWorkbook() : new HSSFWorkbook();
		
		Sheet sheet = workbook.createSheet(yearMonth.toString());
		 
		log.debug("Creating fonts");
		
		Font cycleShiftFont = workbook.createFont();
		cycleShiftFont.setFontName(csFontName);
		cycleShiftFont.setFontHeightInPoints(csFontSize);
		cycleShiftFont.setBold(csFontIsBold);

		Font shiftFont = workbook.createFont();
		shiftFont.setFontName(sFontName);
		shiftFont.setFontHeightInPoints(sFontSize);
		shiftFont.setBold(sFontIsBold);
		
		log.debug("Creating styles");
		
		CellStyle baseStyle = workbook.createCellStyle();
		baseStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
		baseStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		baseStyle.setWrapText(true);
		baseStyle.setAlignment(HorizontalAlignment.CENTER);
		baseStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		baseStyle.setBorderTop(BorderStyle.THIN);
		baseStyle.setBorderRight(BorderStyle.THIN);
		baseStyle.setBorderBottom(BorderStyle.THIN);
		baseStyle.setBorderLeft(BorderStyle.THIN);
		baseStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
		baseStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
		baseStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		baseStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		baseStyle.setFont(shiftFont);
		
		CellStyle nonWorkingDayStyle = workbook.createCellStyle();
		nonWorkingDayStyle.cloneStyleFrom(baseStyle);
		nonWorkingDayStyle.setFont(cycleShiftFont);
		nonWorkingDayStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
		
		CellStyle cycleShiftStyle = workbook.createCellStyle();
		cycleShiftStyle.cloneStyleFrom(baseStyle);
		cycleShiftStyle.setFont(cycleShiftFont);
		cycleShiftStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
		
		CellStyle shiftWithCycleShiftStyle = workbook.createCellStyle();
		shiftWithCycleShiftStyle.cloneStyleFrom(baseStyle);
		shiftWithCycleShiftStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
		
		CellStyle shiftWithoutCycleShiftStyle = workbook.createCellStyle();
		shiftWithoutCycleShiftStyle.cloneStyleFrom(baseStyle);
		shiftWithoutCycleShiftStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
		
		CellStyle consultationStyle = workbook.createCellStyle();
		consultationStyle.cloneStyleFrom(baseStyle);
		consultationStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
		
		log.debug("Starting to add data to the sheet");
		
 		Row row = null;
 		Cell cell = null;
 		int maxGeneratedCols = 1;
 		int currCol = 1;
		for (ScheduleDay day : schedule.getDays()) {
			log.debug("Generating row for day: " + day);
			row = sheet.createRow(day.getDay()-1);
			cell = row.createCell(0);
			cell.setCellValue(day.getDay());
			if (day.getIsWorkingDay()) {
				cell.setCellStyle(cycleShiftStyle);
			}
			
			log.debug("Adding cycle shifts");
			for (Doctor doctor : day.getCycle()) {
				log.debug("The doctor " + doctor + " has a cycle shift");
				cell = row.createCell(currCol);
				cell.setCellValue(doctor.getLastNames());
				if (day.getIsWorkingDay()) {
					cell.setCellStyle(cycleShiftStyle);
				}
				currCol += 1;
			}
			
			if (!day.getIsWorkingDay()) {
				log.debug("The day is not a working day");
				row.setRowStyle(nonWorkingDayStyle);
			} else {
				log.debug("The day is a working day");
				log.debug("Adding regular shifts");
				List<Doctor> doctorsWithShiftAndNotCycleShift = new LinkedList<>();
				for (Doctor doctor : day.getShifts()) {
					log.debug("The doctor " + doctor + " has a regular shift");
					if (day.getCycle().contains(doctor)) {
						log.debug("The regular shift is associated to a cycle shift");
						cell = row.createCell(currCol);
						cell.setCellValue(doctor.getLastNames());
						cell.setCellStyle(shiftWithCycleShiftStyle);
						currCol += 1;
					} else {
						log.debug("The regular shift is not associated to a cycle shift");
						doctorsWithShiftAndNotCycleShift.add(doctor);
					}
				}
				log.debug("Adding doctors with a regular shift and not a cycle shift");
				for (Doctor doctor : doctorsWithShiftAndNotCycleShift) {
					cell = row.createCell(currCol);
					cell.setCellValue(doctor.getLastNames());
					cell.setCellStyle(shiftWithoutCycleShiftStyle);
					currCol += 1;
				}
				
				log.debug("Adding consultations");
				for (Doctor doctor : day.getConsultations()) {
					log.debug("The doctor " + doctor + " has a consultation");
					cell = row.createCell(currCol);
					cell.setCellValue(doctor.getLastNames());
					cell.setCellStyle(consultationStyle);
					currCol += 1;
				}
			}
			
			maxGeneratedCols = currCol > maxGeneratedCols ? currCol : maxGeneratedCols;
			currCol = 1;
		}
		
		// Configure columns
		sheet.setColumnWidth(0, firstColWidth);
		sheet.setDefaultColumnStyle(0, baseStyle);
		log.debug("The maximum columns generated in a row are: " + maxGeneratedCols);
		for (int i = 1; i < maxGeneratedCols; i++) {
			sheet.setColumnWidth(i, colWidth);
			sheet.setDefaultColumnStyle(i, baseStyle);
		}
		
		log.info("Excel generation finished. Writing the workbook to an output stream");
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		workbook.write(outputStream);
		workbook.close();

		
		return outputStream;
	}
	
	public Schedule fromExcel(InputStream inputStream, boolean isXlsx) {
		// TODO
		return null;
	}
}
