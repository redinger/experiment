(function() {
  var openLoginDialog, showLoginDialog;
  openLoginDialog = function(d) {
    var h, title;
    this.container = d.container[0];
    d.overlay.show();
    d.container.show();
    $('#dialog-modal-content', this.container).show();
    title = $('.dialog-modal-title', this.container);
    title.show();
    h = $('.dialog-modal-data', this.container).height() + title.height() + 30;
    $('#dialog-container').height(h);
    $('div.close', this.container).show();
    return $('.dialog-modal-data', this.container).show();
  };
  showLoginDialog = function(e) {
    e.preventDefault();
    return $('#dialog-modal-content').modal({
      overlayId: 'dialog-overlay',
      containerId: 'dialog-container',
      position: [100],
      closeHTML: null,
      minHeight: 80,
      opacity: 60,
      overlayClose: true,
      onOpen: openLoginDialog
    });
  };
  $(document).ready(function() {
    return $('.login-link').bind('click', showLoginDialog);
  });
}).call(this);
