// Adding classes for Sam UI changes  bugid:796 -Qu  1/19/10
//Need to change tier3 to tracsSamTier3 class to avoid sakai styling for tier3 

jQuery(function($){
$('div.tier3').removeClass("tier3").addClass("tracsSamTier3");
$("div.tracsSamTier3:empty").parent().parent().addClass("answered");
$('table.allQuestions tr:not(.answered)').addClass("reviewOrUnanswered");
$('.allQuestions td:first-child').addClass("icons");
$('.allQuestions td:nth-child(2)').addClass("questions");
$("tr.answered td.icons").append('<img src="/samigo-app/images/tree/whiteSpace.png">');
})
