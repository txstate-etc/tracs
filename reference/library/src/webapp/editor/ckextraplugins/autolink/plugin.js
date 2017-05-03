/**
 * Plugin that converts urls to link elements. It works by triggering on space or enter key presses and then
 * checking if the word before the caret position matches the url regex.
 */
(function() {
  var urlRegex = /^(https?:\/\/|www\.)[^\s\/$.?#].[^\s]*$/;

  CKEDITOR.plugins.add('autolink', {
    init: function(editor) { 
      editor.on('contentDom', function() {
        editor.on("key", function(e) {
          if (editor.mode == 'wysiwyg' && (e.data.keyCode == 32 || e.data.keyCode == 13)) {
            var selection = editor.getSelection().getNative();
            var range = selection.getRangeAt(0).cloneRange();

            if (range.startContainer.nodeType != Node.TEXT_NODE) return;

            // Only check for link to replace if no text is selected
            if (range.startOffset != range.endOffset) return;

            // Don't create nested a tags
            if (range.startContainer.parentElement.tagName == 'A') return;

            var text = range.startContainer.nodeValue;

            // Append text from all text node siblings before this one since a word might
            // be split across multiple text nodes.
            var offset = range.startOffset;
            function concatSiblings(prev) {
              if (!prev || prev.nodeType != Node.TEXT_NODE) return;
              text = prev.nodeValue + text;
              offset += prev.length;
              concatSiblings(prev.previousSibling);
            }
            concatSiblings(range.startContainer.previousSibling);

            var textAfter = '';
            if (offset < text.length-1) {
              textAfter = text.substring(offset);
            }
            text = text.substring(0, offset);

            var textBefore = '';
            var spaceIndex = Math.max(text.lastIndexOf(' '), text.lastIndexOf('\xa0'));
            if (spaceIndex != -1) {
              textBefore = text.substring(0, spaceIndex+1);
              text = text.substring(spaceIndex+1);
            }

            // ckeditor sometimes adds zero-width spaces in Chrome. Strip these out before
            // matching regex.
            text = text.replace('\u200B', '');
            if (text.match(urlRegex)) {
              var a = document.createElement('a');
              var textAfterA = document.createTextNode(textAfter);
              var textBeforeA = document.createTextNode(textBefore);
              a.innerHTML = text;
              if (!text.startsWith('http')) text = 'http://' + text;
              a.href = text;

              // TODO: figure out how to get undo snapshots working properly

              // Text to be replaced might be in the middle of some existing text.
              // Make sure that text before/after the link gets added as text nodes
              // before/after the new a element. First, remove all text node siblings
              // before the caret position.
              function removeSiblings(node) {
                if (!node || node.nodeType != Node.TEXT_NODE) return;
                removeSiblings(node.previousSibling);
                node.remove();
              }
              removeSiblings(range.startContainer);
              range.insertNode(a);
              a.parentNode.insertBefore(textBeforeA, a);
              a.parentNode.insertBefore(textAfterA, a.nextSibling);
              range.setStart(a.nextSibling, 0);
              range.collapse(true);
              selection.removeAllRanges();
              selection.addRange(range);
            }
          }
        });

        editor.on('paste', function(e) {
          if (e.data.dataValue.match(urlRegex)) {
            e.data.dataValue = '<a href="' + e.data.dataValue + '">' + e.data.dataValue + '</a>';
          }
        });
      });
    }
  });
})();
