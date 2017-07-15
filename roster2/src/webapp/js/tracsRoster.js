(function ($) {
	roster.readyHideUnhide = function(){
			$("#expandAll").click(function(){
				$('.inactivePar').show();
			});
			$("#collapseAll").click(function(){
				$('.inactivePar').hide();
			});
  };
})(jQuery);
