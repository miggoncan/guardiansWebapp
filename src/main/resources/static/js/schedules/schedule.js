$('#confirmForm').submit(function(event) {
	return confirm($('#confirmMessage').html());
});

// The file input will display the filename when updated
$(".custom-file-input").on("change", function() {
  var fileName = $(this).val().split("\\").pop();
  $(this).siblings(".custom-file-label").addClass("selected").html(fileName);
});