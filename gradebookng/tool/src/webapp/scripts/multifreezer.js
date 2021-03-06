$(document).ready(function () {
/*
jQuery MultiFreezer - scrollable tables with freezed thead and (n) first columns.
(c) 2017 Jan Renner (http://janrenner.cz, jan.renner@gmail.com)
*/
$('.table-freeze-multi').each(function () {

    var table = $(this),
        scrollbarWidth = freezerGetScrollbarWidth();

    //prepare
    table.css({
        margin: 0
    }).addClass('table-freeze-multi-original');

    //wrap
    table.wrap('<div class="freeze-multi-scroll-wrapper" />');
    var wrapper = table.closest('.freeze-multi-scroll-wrapper');
    table.wrap('<div class="freeze-multi-scroll-table" />');
    table.wrap('<div class="freeze-multi-scroll-table-body" />');
    var scroller = wrapper.find('.freeze-multi-scroll-table-body');

    //layout
    var headblock = $('<div class="freeze-multi-scroll-table-head-inner" />');
    scroller.before($('<div class="freeze-multi-scroll-table-head" />').append(headblock));
    var topblock = $('<div class="freeze-multi-scroll-left-head" />');
    var leftblock = $('<div class="freeze-multi-scroll-left-body-inner" />');
    wrapper.append(
        $('<div class="freeze-multi-scroll-left" />')
            .append(topblock)
            .append($('<div class="freeze-multi-scroll-left-body" />').append(leftblock))
    );

    //cloning
    var clone = table.clone(true);
    clone.addClass('table-freeze-multi-clone').removeClass('table-freeze-multi-original');
    var colsNumber = table.data('colsNumber') || table.find('tbody tr:first th').length;

    clone.on("click", function(event) {
        if(event.target.id != "") {
            table.find('#' + event.target.id).trigger("click");
        }
        else if (event.target.parentNode.id != "") {
            table.find('#' + event.target.parentNode.id).trigger("click");
        }
    });

    //head
    var cloneHead = clone.clone(true);
    cloneHead.find('tbody').remove();
    cloneHead.find('tfoot').remove();
    headblock.append(cloneHead);

    //top
    var cloneTop = cloneHead.clone(true);
    topblock.append(cloneTop);

    cloneHead.addClass('table-freeze-head');
    cloneTop.addClass('table-freeze-topLeft');

    //left (contains body and functional footer)
    var cloneLeft = clone.clone(true);
    cloneLeft.find('thead').remove();
    cloneLeft.addClass('table-freeze-left');
    leftblock.append(cloneLeft);

    //remove the duplicate footer from the original table by emptying the divs, adding br's to maintain alignment
    table.find('tfoot').find('div').empty().append("<br>").next().empty().append("<br>");

		//sizing
    var scrollHeight = table.data('scrollHeight') || wrapper.parent().closest('*').height();
    var headerHeight = table.find('thead').height();
    var leftWidth = (function () {
        var w = 0;
        table.find('tbody tr:first > *').slice(0, colsNumber).each(function () {
            w = w + $(this).outerWidth();
        });
        return w + 1;
    }());
    wrapper.css('height', scrollHeight);
    scroller.css('max-height', scrollHeight - headblock.height());
    headblock.width(table.width()).css('padding-right', scrollbarWidth);
    leftblock.add(leftblock.parent()).height(scrollHeight - scrollbarWidth - headerHeight);
    leftblock.width(leftWidth + scrollbarWidth);
    wrapper.find('.freeze-multi-scroll-left').width(leftWidth);

    //postprocess
    wrapper.find('.table-freeze-multi-original thead').hide();

		//scrolling
    scroller.on('scroll', function () {
        var s = $(this),
            left = s.scrollLeft(),
            top = s.scrollTop();
        headblock.css('transform', 'translate(' + (-1 * left) + 'px, 0)');
        leftblock.scrollTop(top);
    });
    leftblock.on('mousewheel', false);

	});
});

// @see https://davidwalsh.name/detect-scrollbar-width
function freezerGetScrollbarWidth () {
    // Create the measurement node
    var scrollDiv = document.createElement("div");
    scrollDiv.className = "freezer-scrollbar-measure";
    document.body.appendChild(scrollDiv);

    // Get the scrollbar width
    var scrollbarWidth = scrollDiv.offsetWidth - scrollDiv.clientWidth;
    //console.warn(scrollbarWidth); // Mac: 15, Win: 17

    // Delete the DIV 
    document.body.removeChild(scrollDiv);

    return scrollbarWidth;
}
