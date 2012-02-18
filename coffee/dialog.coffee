root = exports ? this

# Given a rendered dialog in the DOM, size and display it
openDialog = (d) ->
        container = d.container[0]
        d.overlay.show()
        d.container.show()
        $('#dialog-modal-content', container).show()
        title = $('.dialog-modal-title', container)
        title.show()
        h = $('.dialog-modal-data', container).height() + title.height() + 30
        $('#dialog-container').height(h)
        $('div.close', container).show()
        $('.dialog-modal-data', container).show()

# Given a click or other event, render and show a dialog
root.renderDialog = (title, body, args) ->
        $('.dialog-modal-title').html title
        $('.dialog-modal-data').html body if typeof body == "string"
        $('.dialog-modal-data').html body args if typeof body == "function"
        $('#dialog-modal-content').modal
                overlayId: 'dialog-overlay'
                containerId: 'dialog-container'
                position: [100]
                closeHTML: null
                minHeight: 80
                opacity: 60
                overlayClose: true
                onOpen: openDialog

root.cancelDialog = () ->
        $('#dialog-container').hide()

# Home page dialog events
showLoginDialog = (e) ->
        e.preventDefault()
        loginBody = $('#login-dialog-body').html()
        template = Handlebars.compile loginBody
        root.renderDialog "Login", template, {}

showRegisterDialog = (e) ->
        e.preventDefault()
        regBody = $('#register-dialog-body').html()
        template = Handlebars.compile regBody
        root.renderDialog "Register", template,
                "target": $(e).attr('href')
                "default": window.location.pathname

showForgotPasswordDialog = (e) ->
        e.preventDefault()
        regBody = $('#forgot-password-body').html()
        template = Handlebars.compile regBody
        root.renderDialog "Forgot Password", template,
                "target": $(e).attr('href')
                "default": window.location.pathname

$(document).ready ->
        $('.login-link').bind 'click', showLoginDialog
        $('.forgot-password').bind 'click', showForgotPasswordDialog
        $('.register-link').bind 'click', showRegisterDialog
        $('.cancel-button').bind 'click', cancelDialog

# Simple dynamic inline views

# Any clickable element with .show-dynform will result in siblings
# with .dynform being shown

showReplyForm = (e) ->
        e.preventDefault()
        targ = $(e.target)
        targ.siblings('.comment-form').show()
        targ.hide()

$(document).ready ->
        $('.show-dform').bind 'click', showReplyForm