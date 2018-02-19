$(document).ready(function(){

		$("tr.inactivePar").css("display","none");
		$("tr.inactivePar").addClass("inactive", "inactiveBack");

		$("#expandAll").click(function(){
			$('.inactivePar').show();
		});
		$("#collapseAll").click(function(){
			$('.inactivePar').hide();
		});
});