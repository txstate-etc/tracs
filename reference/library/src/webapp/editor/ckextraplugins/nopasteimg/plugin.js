/**
 * Simple plugin to disable image paste and image drag and drop.
 * Antisamy won't allow img tags with data directly in the source.
 *
 * This only really applies to Firefox. Chrome and IE both open the
 * image in a new tab instead of dropping directly into ckeditor.
 * Also, Chrome and IE will not convert image paste to <img src="data:*"/> format.
 */
(function() {
  var imgRegex = /<img[^>]*src="data:image\/[a-z]*;base64,.*?"[^>]*>/gi;

  CKEDITOR.plugins.add('nopasteimg', {
    init: function(editor) {
      editor.on('contentDom', function() {
        editor.document.on('drop', function(e) {
          // Prevent default if dropping any files
          if (e.data.$.dataTransfer.files.length) {
            e.data.preventDefault(true);
            alert('File dragdrop is not supported by this editor.');
          }
        });

        editor.on('paste', function(e) {
          if (e.data.dataValue.match(imgRegex)) {
            e.stop();
            alert('Image paste is not supported by this editor.');
          }
        });
      });
    }
  });
})();
