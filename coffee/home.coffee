openLoginDialog = (d) ->
        @container = d.container[0]
        d.overlay.show()
        d.container.show()
        $('#dialog-modal-content', @container).show()
        title = $('.dialog-modal-title', @container)
        title.show()
        h = $('.dialog-modal-data', @container).height() + title.height() + 30
        $('#dialog-container').height(h)
        $('div.close', @container).show()
        $('.dialog-modal-data', @container).show()

showLoginDialog = (e) ->
        e.preventDefault()
        $('#dialog-modal-content').modal
                overlayId: 'dialog-overlay'
                containerId: 'dialog-container'
                position: [100]
                closeHTML: null
                minHeight: 80
                opacity: 60
                overlayClose: true
                onOpen: openLoginDialog

$(document).ready ->
        $('.login-link').bind 'click', showLoginDialog