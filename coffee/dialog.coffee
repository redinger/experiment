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
  id: 'modalDialog'
  initialize: ->
        super()
        @

  render: =>
        @$el.html @template
                id: 'modalDialog'
                header: '<h2>' + options.header + '</h2>'
                body: options.message
                footer: "<a class='btn accept'>Ok</a>"
        @$el.css('display', 'none')
        @

  showMessage: (data) =>
        if data
           options.header = data.header
           options.message = data.message
        $('#modalDialog').remove()
        $('.templates').append @render().el

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
        @

  events:
        'keyup': 'handleKey'
        'click .cancel': 'cancel'
        'click .login': 'login'
        'click .forgot': 'forgot'

  enterPressed: -> @login()
  login: =>
        if not @form.validate()
           $.post '/action/login', @form.getValue(), @serverValidate

  serverValidate: (data) =>
        if data.result == "fail"
            @form.fields["password"].setError(data.message || "Unknown Error")
        else
            window.location.pathname = "/"
        @cancel()

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
        'keyup #username': 'handleUsername'
        'keyup #email': 'handleEmail'
        'click .register': 'register'
        'click .cancel': 'cancel'

  handleUsername: =>
        $.ajax
           url: '/action/check-username'
           data: { username: @form.getValue().username }
           success: @usernameValidate
           timeout: 500

  usernameValidate: (data) =>
        if data.exists == "true"
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
            @form.fields['email'].setError("This address is already registered")
        else
            @form.fields['email'].clearError()

  enterPressed: -> @register()
  register: =>
        if not @form.validate()
           $.post '/action/register', @form.getValue(), @serverValidate

  serverValidate: (data) =>
        if data.result == "fail"
           @form.fields['password2'].setError(data.message || "Unknown Error")
        else
           @hide()
           window.modalDialog

window.regModal = new RegisterModal()

#
# Forgot Password Dialog
# -----------------------------

# Setup global modal events (menus, nav, etc)
$(document).ready ->
        $('.login-button').bind 'click',
                (e) ->
                        e.preventDefault()
                        window.loginModal.show()

        $('.register-button').bind 'click',
                (e) ->
                        e.preventDefault()
                        window.regModal.show()
        $('.show-dform').bind 'click',
                (e) ->
                        e.preventDefault()

        $('#homeCarousel').carousel interval: 10000
