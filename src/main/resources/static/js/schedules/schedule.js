$('#confirmForm').submit(function(event) {
	return confirm($('#confirmMessage').html());
});