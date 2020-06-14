// Search bar
$(document).ready(function(){
  $("#searchDoctorBar").on("keyup", function() {
    var value = $(this).val().toLowerCase();
    $("#doctorTable tr:has(td)").filter(function() {
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