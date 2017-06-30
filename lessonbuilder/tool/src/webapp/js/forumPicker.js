$(document).ready(function() {
  $('#list div[role="listitem"]').each(function() {
    $link = $(this).find('a');
    if ($link.attr('href').startsWith('/direct/forum/')) {
      $(this).addClass('forum');
    } else {
      $(this).addClass('topic');
    }
  });
});
