jQuery(document).ready(function($) {
	// Initially hide the showLessButton
	$('#showLessButton').hide();
});
// Show more and show less buttons
$('#showMoreButton').click(function() {
	$(this).hide();
	$('#showLessButton').show();
});
$('#showLessButton').click(function() {
	$(this).hide();
	$('#showMoreButton').show();
});