$(document).ready(function(){

		$("tr.inactivePar").css("display","none");

		$("#expandAll").click(function(){
			$('.inactivePar').show();
		});
		$("#collapseAll").click(function(){
			$('.inactivePar').hide();
		});
});