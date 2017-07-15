(function ($) {

  function getCountMessage(numActive, numInactive) {
    var countMessage = 'Currently viewing ' + numActive + ' active participants';
    if (numInactive > 0) {
      countMessage += ' and ' + numInactive + ' inactive participants';
    }
    return countMessage;
  }

	roster.readyHideUnhide = function(){
			$("#expandAll").click(function(){
				$('.inactivePar').show();
			});
			$("#collapseAll").click(function(){
				$('.inactivePar').hide();
			});

      if ($('.rosterRoleTopLine').length) {
        $('.rosterRoleTopLine').html(getCountMessage($('.activePar').length, $('.inactivePar').length));
      }
      if ($('.rosterRoleTopLineGrouped').length) {
        $('.rosterRoleTopLineGrouped').each(function() {
          var $rosterTable = $(this).closest('table').next();
          $(this).html(getCountMessage($rosterTable.find('.activePar').length, $rosterTable.find('.inactivePar').length));
        });
      }
  };
})(jQuery);
