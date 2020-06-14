jQuery(document).ready(function($) {
	$('#startDate').datepicker({
		format: $('#startDate').data('format'),
		language: $('#startDate').data('language'),
		orientation: $('#startDate').data('orientation'),
		weekStart: 1 // Monday
	});
	$('#showLessButton').hide();
	if ($('#doesShifts').is(':checked')) {
		$('#collapseShifts').collapse('show');
		$('#collapseShowMore').collapse('show');
	}
	if (!$('#hasShiftsOnlyWhenCycleShifts').is(':checked')) {
		$('#collapseShiftsNum').collapse('show');
		$('#collapseShiftPreferences').collapse('show');
	}
	if ($('#doesConsultations').is(':checked')) {
		$('#collapseConsultations').collapse('show');
		$('#collapseConsultationPreferences').collapse('show');
		$('#collapseShowMore').collapse('show');
	}
	
	// Show more and show less buttons
	$('#showMoreButton').click(function() {
		$(this).hide();
		$('#showLessButton').show();
	});
	$('#showLessButton').click(function() {
		$(this).hide();
		$('#showMoreButton').show();
	});
	
	// This function will show the collapse #collapseShowMore depending on the state
	// of #doesShifts and #doesConsultations and #hasShiftsOnlyWhenCycleShifts
	const checkToggleCollapseShowMore = function() {
		if(($('#doesShifts').is(':checked') 
				&& !$('#hasShiftsOnlyWhenCycleShifts').is(':checked'))
				|| $('#doesConsultations').is(':checked')) {
			if(!$('#collapseShowMore').is('.show')) {
				$('#collapseShowMore').collapse('show');
			}
		} else {
			$('#collapseShowMore').collapse('hide');
		}
	}
	$('#doesShifts').click(checkToggleCollapseShowMore);
	$('#hasShiftsOnlyWhenCycleShifts').click(checkToggleCollapseShowMore);
	$('#doesConsultations').click(checkToggleCollapseShowMore);
	
	// This function will decide the state of the #collapseShiftPreferences depending
	// on the state of #doesShifts and #hasShiftsOnlyWhenCycleShifts
	const checkToggleCollapseShiftPreferences = function() {
		if ($('#doesShifts').is(':checked') 
				&& !$('#hasShiftsOnlyWhenCycleShifts').is(':checked')) {
			if(!$('#collapseShiftPreferences').is('.show')) {
				$('#collapseShiftPreferences').collapse('show');
			}
		} else {
			$('#collapseShiftPreferences').collapse('hide');
		}
	};
	$('#doesShifts').click(checkToggleCollapseShiftPreferences);
	$('#hasShiftsOnlyWhenCycleShifts').click(checkToggleCollapseShiftPreferences);
});

$('#startDate').focusout(function() {
	if (!Date.parse($(this).val())) {
		$(this).addClass('is-invalid');
	} else {
		$(this).removeClass('is-invalid');
	}
});

$('#maxShifts').focusout(function() {
	if ($('#minShifts').val() > $(this).val()) {
		$(this).addClass('is-invalid');
	} else {
		$(this).removeClass('is-invalid');
	}
});