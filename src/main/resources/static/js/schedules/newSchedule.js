// Initialization of components
$('#datepicker').datepicker({
	format: $('#datepicker').data('format'),
	language: $('#datepicker').data('language'),
	maxViewMode: 0, // Only show the days of the month
	autoclose: true, // Close when a date is selected
	multidate: true,
	weekStart: 1 // Monday
});
$('selectpicker').selectpicker();
  
// Preselect the non working days
(() => {
	const yearMonth = $('#dataMonth').attr('data-month-conf-year-month');
	var nonWorkingDays = [];
	$('#dataMonth').children().each(function() {
	    if($(this).attr('data-day-conf-is-working-day') == 'false') {
	    	const dayConfDate = yearMonth + '-' + $(this).attr('data-day-conf-day');
	    	nonWorkingDays.push(dayConfDate);
	    }
	});
	// The update method does not accept a list of strings, so it has to be unpacked
	$('#datepicker').datepicker('update', ...nonWorkingDays);
})();

// Show the modal when right clicking/holding on a day
const addListeners = function() {
	$('td.day:not(.disabled)').contextmenu(function() {
		const dayNum = $(this).html();
		const dataDay = $('#dataDay' + dayNum);
		$('#editDayModalDay').html(dayNum);
		$('#editDayModalMinShifts').val(
				dataDay.attr('data-day-conf-min-shifts'));
		$('#editDayModalMinConsultations').val(
				dataDay.attr('data-day-conf-min-consultations'));
		$('#editDayModalWantedShifts').selectpicker('val', 
				JSON.parse(dataDay.attr('data-day-conf-wanted-shifts-values')));
		$('#editDayModalUnwantedShifts').selectpicker('val', 
				JSON.parse(dataDay.attr('data-day-conf-unwanted-shifts-values')));
		$('#editDayModal').modal('show');
		return false;
	});
};
// Every time a date is selected, the listeners have to be added again
$('#datepicker').on('changeDate', addListeners);
addListeners();

// Initially hide the progress bar
//$('#submitProgress').hide();

// Save the data of the modal on confirm
$('#confirmDayConf').click(function(){
	var dataDay = $('#dataDay' + $('#editDayModalDay').text());
	dataDay.attr('data-day-conf-min-shifts', 
			$('#editDayModalMinShifts').val());
	dataDay.attr('data-day-conf-min-consultations', 
			$('#editDayModalMinConsultations').val());
	dataDay.attr('data-day-conf-wanted-shifts-values', 
			JSON.stringify($('#editDayModalWantedShifts').val()));
	dataDay.attr('data-day-conf-unwanted-shifts-values', 
			JSON.stringify($('#editDayModalUnwantedShifts').val()));
	$('#editDayModal').modal('hide');
});

// Send the request on confirm
$('#submitBtn').click(function() {
	if (confirm($('#confirmMessage').html())) {
		$('#submitProgress').show();
		$("#submitProgressBar").animate({width: "80%"}, 3000);
		var dayConfs = []
		$('#dataMonth').children().each(function() {
			var dayConf = {
				day: $(this).attr('data-day-conf-day'),
				isWorkingDay: true, // This is changed afterwards
				numShifts: $(this).attr('data-day-conf-min-shifts'),
				numConsultations: $(this).attr('data-day-conf-min-consultations'),
				wantedShifts: [],
				unwantedShifts: []
			};
			$.each(JSON.parse($(this).attr('data-day-conf-wanted-shifts-values')), 
				function(i, docId) {
					dayConf.wantedShifts.push({id: parseInt(docId)});					
				});
			$.each(JSON.parse($(this).attr('data-day-conf-unwanted-shifts-values')), 
				function(i, docId) {
					dayConf.unwantedShifts.push({id: parseInt(docId)});					
				});
			dayConfs.push(dayConf);
		});
		$.each($('#datepicker').datepicker('getFormattedDate').split(','), 
			function (i, nonWorkingDate) {
				var dayNumStr = nonWorkingDate.split('-')[2];
				var dayConfIndex = parseInt(dayNumStr) - 1;
				dayConfs[dayConfIndex].isWorkingDay = false;
			});
		console.log(dayConfs);
		$.ajax({
			type: "POST",
			url: window.location.href, 
			data: JSON.stringify(dayConfs),
			contentType: "application/json; charset=utf-8",
			success: data => window.location = data.scheduleHref 
		});
	}
});