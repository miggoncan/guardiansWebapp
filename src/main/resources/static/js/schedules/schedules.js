// Initialize the datepicker 
$('#yearMonthPicker').datepicker({
	format: $('#yearMonthPicker').data('format'),
	language: $('#yearMonthPicker').data('language'),
	orientation: $('#yearMonthPicker').data('orientation'),
	autoclose: true,
	minViewMode: 1, // Select a month
	weekStart: 1 // Monday
});
$('#yearMonthPicker').on('changeDate', function() {
	var selectedDate = $(this).datepicker('getFormattedDate');
	console.log(selectedDate.substring(0, selectedDate.length-3));
    $('#yearMonth').val(selectedDate.substring(0, selectedDate.length-3));
});
// Search bar
$(document).ready(function(){
  $("#searchScheduleBar").on("keyup", function() {
    var value = $(this).val().toLowerCase();
    $("#scheduleTable tr:has(td)").filter(function() {
      $(this).toggle($(this).text().toLowerCase().indexOf(value) > -1)
    });
  });
});
// Redirect when clicking a row
jQuery(document).ready(function($) {
    $(".clickable-row").click(function() {
        window.location = $(this).data("href");
    });
});