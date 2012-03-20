root = exports ? this

#
# Modal Dialog Base
# ------------------------------

class ModalView extends Backbone.View
  initialize: (opts) ->
      @template = Handlebars.compile $('#modal-dialog-template').html()
      @

  show: =>
        @$el.modal('show')

  hide: =>
        @$el.modal('hide')

  handleKey: (e) =>
        if e.which == 13
           e.preventDefault()
           @enterPressed() if @enterPressed

class ModalMessage extends ModalView
  attributes:
        id: 'modalDialogWrap'
        class: 'modal hide modalDialogWrap'
  initialize: ->
        super()
        @

  render: =>
        @$el.html @template
                id: 'modalDialog'
                header: '<h2>' + @options.header + '</h2>'
                body: @options.message
                footer: "<a class='btn accept'>Ok</a>"
        @$el.css('display', 'none')
        @

  showMessage: (data) =>
        if data
           @options.header = data.header
           @options.message = data.message
        $('.modalDialogWrap').remove()
        $('.templates').append @render().el
        @show()

  events:
        'keyup': 'handleKey'
        'click .accept': 'hide'

window.modalMessage = new ModalMessage({header: "", message: ""})


# Abstract out modal dialogs with form-based bodies

class ModalForm extends ModalView
  initialize: ->
        super()
        if @schema
           @makeForm @schema
           $('.templates').append @render().el
           @$el.css('display', 'none')
        else
           alert 'schema not initialized'

  makeForm: (schema, data) ->
        @form = new Backbone.Form
                schema: schema
                data: data || {}
        @

  clearForm: ->
        vals = {}
        _(@schema).map( (k,v) -> vals[k] = "" if k.length > 0)
        @form.setValue vals
        @

  cancel: =>
        @clearForm()
        @hide()



# Login Module
# -----------------------------

loginSchema =
   username:
      title: "Username or E-mail"
      validators: ['required']
   password:
      type: "Password"
      title: "Password"
      validators: ['required']

class LoginModal extends ModalForm
  id: 'loginModal'
  className: 'modal hide fade'
  initialize: ->
        @schema = loginSchema
        super()
        @

  render: =>
        @$el.html @template
                id: 'loginModal'
                header: '<h1>Login</h1>'
                footer: "<a class='btn btn-primary login'>Login</a>
                         <a class='btn cancel'>Cancel</a>"
        @$('.modal-body').append @form.render().el
        @$('.modal-body').append "<p class='forgot-line'> <a class='forgot'>Forgot your username or password?</a></p>"
#        @$('.modal-body').append "<div class='fb-login-button' data-scope='email'>Connect</div>"
        @

  events:
        'keyup': 'handleKey'
        'click .cancel': 'loginCancel'
        'click .close': 'loginCancel'
        'click .login': 'login'
        'click .forgot': 'forgot'

  enterPressed: -> @login()
  login: =>
        if not @form.validate()
           $.post '/action/login', @form.getValue(), @serverValidate

  loginCancel: () =>
        if window.location.search.length > 0
           window.location.href = window.location.protocol + "//" + window.location.host + "/"
        else
           @cancel()

  serverValidate: (data) =>
        if data.result == "fail"
            @form.fields["password"].setError(data.message || "Unknown Error")
        else
            target = window.queryParams['target']
            if target
                window.location.pathname = target
            else
                window.location.pathname = "/"

  forgot: =>
        @hide()
        window.forgotDialog.show()


window.loginModal = new LoginModal()


#
# Registration Dialog
# -------------------------------

regSchema =
  email:
     title: "E-mail Address"
     validators: ['email', 'required']
  username:
     title: "Choose a username"
     validators: ['required']
  name:
     title: "Your full name"
  password:
     title: "Choose a password"
     type: "Password"
     validators: ['required']
  password2:
     title: "Re-enter password"
     type: "Password"
     validators: ['required',
                     type: 'match'
                     field: 'password'
                     message: 'Passwords must match']

class RegisterModal extends ModalForm
  id: 'regModal'
  className: 'modal hide fade'
  initialize: ->
        @schema = regSchema
        super()
        @

  render: =>
        @$el.html @template
                id: 'regModal'
                header: '<h1>Register your Account</h1>'
                footer: "<a class='btn btn-primary register'>Register</a>
                        <a class='btn cancel'>Cancel</a>"
        @$('.modal-body').append @form.render().el
        @

  events:
        'keyup': 'handleKey'
        'click .register': 'register'
        'click .cancel': 'cancel'
        'click .close' : 'cancel'

#         'keyup #username': 'handleUsername'
#        'keyup #email': 'handleEmail'

  handleUsername: =>
        $.ajax
           url: '/action/check-username'
           data: { username: @form.getValue().username }
           success: @usernameValidate
           timeout: 500

  usernameValidate: (data) =>
        if data.exists == "true"
            @form.fields['username'].clearError()
            @form.fields['username'].setError("This username is taken")
        else
            @form.fields['username'].clearError()

  handleEmail: =>
        $.ajax
           url: '/action/check-email'
           data: { email: @form.getValue().email }
           success: @emailValidate
           timeout: 500

  emailValidate: (data) =>
        if data.exists == "true"
            @form.fields['email'].clearError()
            @form.fields['email'].setError("This address is already registered")
        else
            @form.fields['email'].clearError()

  enterPressed: -> @register()
  register: =>
        @handleUsername()
        @handleEmail()
        if not @form.validate()
           $.post '/action/register', @form.getValue(), @serverValidate

  serverValidate: (data) =>
        if data.result == "fail"
           @form.fields['password2'].setError(data.message || "Unknown Error")
        else
           @hide()
           window.modalMessage.showMessage
                header: "Thank you"
                message: "<p>Thank you for registering, you should receive an e-mail confirming your registration shortly.</p>"

window.regModal = new RegisterModal()

#
# Forgot Password Dialog
# -----------------------------



# Startup event handlers and actions
# -----------------------

extractParams = () ->
        qs = document.location.search.split("+").join(" ")
        re = /[?&]?([^=]+)=([^&]*)/g
        params = {}
        params[decodeURIComponent tokens[1]] = decodeURIComponent tokens[2] while tokens = re.exec qs
        params

window.queryParams = extractParams()

$(document).ready ->

# Startup Actions
        $('#homeCarousel').carousel interval: 10000 if $('#homeCarousel').length

# Modals
        $('.login-button').bind 'click',
                (e) ->
                        e.preventDefault()
                        window.loginModal.show()

        $('.register-button').bind 'click',
                (e) ->
                        e.preventDefault()
                        window.regModal.show()

# Various actions
        $('.show-dform').bind 'click',
                (e) ->
                        e.preventDefault()
                        targ = $(e.target)
                        targ.siblings('.comment-form').show()
                        targ.hide()
