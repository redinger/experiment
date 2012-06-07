define ['jquery', 'views/common', 'use!Handlebars', 'use!BackboneFormsBS', 'use!BackboneFormsEditors', 'libs/misc/jstz.min'],
  ($, Common) ->


# Login Dialog
# -----------------------------

    loginSchema =
       username:
          title: "Username or E-mail"
          validators: ['required']
       password:
          type: "Password"
          title: "Password"
          validators: ['required']

    class LoginModal extends Common.ModalForm
      attributes:
        id: 'loginModal'
        class: 'modal hide fade'
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
        @$('.modal-body').append "<p class='forgot-line'> <a class='forgot-pw' href='#forgot'>Forgot your username or password?</a></p>"
#        @$('.modal-body').append "<div class='fb-login-button' data-scope='email'>Connect</div>"
        @delegateEvents()
        @

      events:
        'keyup': 'handleKey'
        'click .cancel': 'loginCancel'
        'click .close': 'loginCancel'
        'click .login': 'login'
        'click .forgot-pw': 'forgot'

      enterPressed: => @login()
      login: =>
        if not @form.validate()
           $.post '/action/login', @form.getValue(), @serverValidate

      loginCancel: () =>
        if window.location.search.length > 0
           window.location.href = window.location.protocol + "//" + window.location.host + "/"
        else
           @cancel()

      serverValidate: (data) =>
        if data.result isnt "success"
            @form.fields["password"].setError(data.message or "Unknown Error")
        else
            target = Common.queryParams['target'] or "/"
            window.location.href = window.location.protocol + "//" + window.location.host + target

      forgot: =>
        @$el.toggleClass 'fade'
        @cancel()
        @$el.toggleClass 'fade'
        forgotModal.show()


    loginModal = new LoginModal()


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

    class RegisterModal extends Common.ModalForm
      attributes:
        id: 'regModal'
        class: 'modal hide fade'
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
        @delegateEvents()
        @

      events:
        'keyup': 'handleKey'
        'click .register': 'register'
        'click .cancel': 'cancel'
        'click .close' : 'cancel'

        'keyup #username': 'checkUsername'
        'keyup #email': 'checkEmail'

      checkUsername: =>
        if not @checkUsernameHandler?
          @checkUsernameHandler =
              _.throttle ->
                $.ajax
                  url: '/action/check-username'
                  data: { username: @form.getValue().username }
                  success: @validateUsername
                  timeout: 2000
                  spinner: false
              , 1000
        @checkUsernameHandler()

      validateUsername: (data) =>
        if data.exists == "true"
            @form.fields['username'].clearError()
            @form.fields['username'].setError("This username is taken")
        else
            @form.fields['username'].clearError()

      checkEmail: =>
        if not @checkEmailHandler?
          @checkEmailHandler =
              _.debounce ->
                $.ajax
                  url: '/action/check-email'
                  data: { email: @form.getValue().email }
                  success: @emailValidate
                  timeout: 2000
                  spinner: false
              , 600
        @checkEmailHandler()

      validateEmail: (data) =>
        if data.exists == "true"
            @form.fields['email'].clearError()
            @form.fields['email'].setError("This address is already registered")
        else
            @form.fields['email'].clearError()

      enterPressed: => @register()
      register: =>
        @checkUsername()
        @checkEmail()
        if not @form.validate()
           $.post '/action/register', @form.getValue(), @serverValidate

      serverValidate: (data) =>
        if data.result isnt "success"
           @form.fields['password2'].setError(data.message || "Unknown Error")
        else
           @cancel()
           Common.modalMessage.showMessage
                header: "Thank you"
                message: "<p>Thank you for registering, you should receive an e-mail confirming your registration shortly.</p>"

    regModal = new RegisterModal()

#
# Forgot Password Dialog
# -----------------------------

    forgotSchema =
      userid:
        title: "User ID or E-mail"
        validators: ['required']

    class ForgotModal extends Common.ModalForm
      attributes:
        id: 'forgotModal'
        class: 'modal hide'
      initialize: ->
        @schema = forgotSchema
        super()
        @

      render: =>
        @$el.html @template
                id: 'forgotModal'
                header: '<h1>Forgot your Password?</h1>'
                footer: '<a class="btn btn-primary reset-pw">Reset Password</a>
                         <a class="btn cancel">Cancel</a>'
        @$('.modal-body').append @form.render().el
        @delegateEvents()
        @

      enterPressed: => @resetPassword()

      events:
        'keyup': 'handleKey'
        'click .reset-pw': 'resetPassword'
        'click .cancel': 'cancel'
        'click .close': 'cancel'

      resetPassword: =>
        if not @form.validate()
                $.post '/action/forgotpw', @form.getValue(), @checkAccount

      checkAccount: (data) =>
        if data.result isnt "success"
                @form.fields['userid'].setError(data.message || "Unknown Error")
        else
                @cancel()
                Common.modalMessage.showMessage
                        header: "Password Reset"
                        message: "<p>Please check your e-mail for your temporary password</p>"

    forgotModal = new ForgotModal()


# ## Home Page Startup Actions
    $(document).ready ->

        # Home page carousel
        $('#homeCarousel').carousel interval: 10000 if $('#homeCarousel').length

        # Modal Binding
        $('.login-button').bind 'click',
                (e) ->
                        e.preventDefault()
                        loginModal.show()

        $('.register-button').bind 'click',
                (e) ->
                        e.preventDefault()
                        regModal.show()

        # Establish this sessions' home TZ
        if not session_timezone?
          timezone = jstz.determine()
          $.ajax
              url: '/action/timezone'
              data: {_timezone: timezone.name()}
              timeout: 2000
              spinner: false
          , 600

# Return the empty set for now
    {}