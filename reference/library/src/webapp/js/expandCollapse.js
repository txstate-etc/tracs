$(document).ready(function(){

		$("tr.inactivePar").css("display","none");
		$("tr.inactivePar").addClass("inactive", "inactiveBack");

		$("#expandAll").click(function(){
			$('i.inactivePar').empty();
			$('.inactivePar').show();
		});
		$("#collapseAll").click(function(){
			$('.inactivePar').hide();
		});
});