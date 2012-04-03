(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; }, __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype;
    child.prototype = new ctor;
    child.__super__ = parent.prototype;
    return child;
  };
  define(['jquery', 'use!Handlebars', 'use!BackboneFormsBS', 'use!BackboneFormsEditors'], function() {
    var Dialogs, ForgotModal, LoginModal, ModalForm, ModalMessage, ModalView, RegisterModal, extractParams, forgotSchema, loginSchema, regSchema;
    if (!Dialogs) {
      Dialogs = {};
    }
    ModalView = (function() {
      __extends(ModalView, Backbone.View);
      function ModalView() {
        this.handleKey = __bind(this.handleKey, this);
        this.hide = __bind(this.hide, this);
        this.show = __bind(this.show, this);
        ModalView.__super__.constructor.apply(this, arguments);
      }
      ModalView.prototype.initialize = function(opts) {
        this.template = Handlebars.compile($('#modal-dialog-template').html());
        return this;
      };
      ModalView.prototype.show = function() {
        return this.$el.modal('show');
      };
      ModalView.prototype.hide = function() {
        return this.$el.modal('hide');
      };
      ModalView.prototype.handleKey = function(e) {
        if (e.which === 13) {
          e.preventDefault();
          if (this.enterPressed) {
            return this.enterPressed();
          }
        }
      };
      return ModalView;
    })();
    ModalMessage = (function() {
      __extends(ModalMessage, ModalView);
      function ModalMessage() {
        this.enterPressed = __bind(this.enterPressed, this);
        this.showMessage = __bind(this.showMessage, this);
        this.render = __bind(this.render, this);
        ModalMessage.__super__.constructor.apply(this, arguments);
      }
      ModalMessage.prototype.attributes = {
        id: 'modalDialogWrap',
        "class": 'modal hide modalDialogWrap'
      };
      ModalMessage.prototype.initialize = function() {
        ModalMessage.__super__.initialize.call(this);
        return this;
      };
      ModalMessage.prototype.render = function() {
        this.$el.html(this.template({
          id: 'modalDialog',
          header: '<h2>' + this.options.header + '</h2>',
          body: this.options.message,
          footer: "<a class='btn accept'>Ok</a>"
        }));
        this.delegateEvents();
        this.$el.css('display', 'none');
        return this;
      };
      ModalMessage.prototype.showMessage = function(data) {
        if (data) {
          this.options.header = data.header;
          this.options.message = data.message;
        }
        this.undelegateEvents();
        $('.modalDialogWrap').remove();
        $('.templates').append(this.render().el);
        return this.show();
      };
      ModalMessage.prototype.enterPressed = function() {
        return this.hide();
      };
      ModalMessage.prototype.events = {
        'keyup': 'handleKey',
        'click .accept': 'hide'
      };
      return ModalMessage;
    })();
    Dialogs.modalMessage = new ModalMessage({
      header: "",
      message: ""
    });
    ModalForm = (function() {
      __extends(ModalForm, ModalView);
      function ModalForm() {
        this.cancel = __bind(this.cancel, this);
        ModalForm.__super__.constructor.apply(this, arguments);
      }
      ModalForm.prototype.initialize = function() {
        ModalForm.__super__.initialize.call(this);
        if (this.schema) {
          this.makeForm(this.schema);
          $('.templates').append(this.render().el);
          return this.$el.css('display', 'none');
        } else {
          return alert('schema not initialized');
        }
      };
      ModalForm.prototype.makeForm = function(schema, data) {
        this.form = new Backbone.Form({
          schema: schema,
          data: data || {}
        });
        return this;
      };
      ModalForm.prototype.clearForm = function() {
        var vals;
        vals = {};
        _(this.schema).map(function(k, v) {
          if (k.length > 0) {
            return vals[k] = "";
          }
        });
        this.form.setValue(vals);
        return this;
      };
      ModalForm.prototype.cancel = function() {
        this.clearForm();
        return this.hide();
      };
      return ModalForm;
    })();
    loginSchema = {
      username: {
        title: "Username or E-mail",
        validators: ['required']
      },
      password: {
        type: "Password",
        title: "Password",
        validators: ['required']
      }
    };
    LoginModal = (function() {
      __extends(LoginModal, ModalForm);
      function LoginModal() {
        this.forgot = __bind(this.forgot, this);
        this.serverValidate = __bind(this.serverValidate, this);
        this.loginCancel = __bind(this.loginCancel, this);
        this.login = __bind(this.login, this);
        this.enterPressed = __bind(this.enterPressed, this);
        this.render = __bind(this.render, this);
        LoginModal.__super__.constructor.apply(this, arguments);
      }
      LoginModal.prototype.attributes = {
        id: 'loginModal',
        "class": 'modal hide fade'
      };
      LoginModal.prototype.initialize = function() {
        this.schema = loginSchema;
        LoginModal.__super__.initialize.call(this);
        return this;
      };
      LoginModal.prototype.render = function() {
        this.$el.html(this.template({
          id: 'loginModal',
          header: '<h1>Login</h1>',
          footer: "<a class='btn btn-primary login'>Login</a>                         <a class='btn cancel'>Cancel</a>"
        }));
        this.$('.modal-body').append(this.form.render().el);
        this.$('.modal-body').append("<p class='forgot-line'> <a class='forgot-pw' href='#forgot'>Forgot your username or password?</a></p>");
        this.delegateEvents();
        return this;
      };
      LoginModal.prototype.events = {
        'keyup': 'handleKey',
        'click .cancel': 'loginCancel',
        'click .close': 'loginCancel',
        'click .login': 'login',
        'click .forgot-pw': 'forgot'
      };
      LoginModal.prototype.enterPressed = function() {
        return this.login();
      };
      LoginModal.prototype.login = function() {
        if (!this.form.validate()) {
          return $.post('/action/login', this.form.getValue(), this.serverValidate);
        }
      };
      LoginModal.prototype.loginCancel = function() {
        if (window.location.search.length > 0) {
          return window.location.href = window.location.protocol + "//" + window.location.host + "/";
        } else {
          return this.cancel();
        }
      };
      LoginModal.prototype.serverValidate = function(data) {
        var target;
        if (data.result !== "success") {
          return this.form.fields["password"].setError(data.message || "Unknown Error");
        } else {
          target = Dialogs.queryParams['target'] || "/";
          return window.location.href = window.location.protocol + "//" + window.location.host + target;
        }
      };
      LoginModal.prototype.forgot = function() {
        this.$el.toggleClass('fade');
        this.cancel();
        this.$el.toggleClass('fade');
        return Dialogs.forgotModal.show();
      };
      return LoginModal;
    })();
    Dialogs.loginModal = new LoginModal();
    regSchema = {
      email: {
        title: "E-mail Address",
        validators: ['email', 'required']
      },
      username: {
        title: "Choose a username",
        validators: ['required']
      },
      name: {
        title: "Your full name"
      },
      password: {
        title: "Choose a password",
        type: "Password",
        validators: ['required']
      },
      password2: {
        title: "Re-enter password",
        type: "Password",
        validators: [
          'required', {
            type: 'match',
            field: 'password',
            message: 'Passwords must match'
          }
        ]
      }
    };
    RegisterModal = (function() {
      __extends(RegisterModal, ModalForm);
      function RegisterModal() {
        this.serverValidate = __bind(this.serverValidate, this);
        this.register = __bind(this.register, this);
        this.enterPressed = __bind(this.enterPressed, this);
        this.emailValidate = __bind(this.emailValidate, this);
        this.handleEmail = __bind(this.handleEmail, this);
        this.usernameValidate = __bind(this.usernameValidate, this);
        this.handleUsername = __bind(this.handleUsername, this);
        this.render = __bind(this.render, this);
        RegisterModal.__super__.constructor.apply(this, arguments);
      }
      RegisterModal.prototype.attributes = {
        id: 'regModal',
        "class": 'modal hide fade'
      };
      RegisterModal.prototype.initialize = function() {
        this.schema = regSchema;
        RegisterModal.__super__.initialize.call(this);
        return this;
      };
      RegisterModal.prototype.render = function() {
        this.$el.html(this.template({
          id: 'regModal',
          header: '<h1>Register your Account</h1>',
          footer: "<a class='btn btn-primary register'>Register</a>                        <a class='btn cancel'>Cancel</a>"
        }));
        this.$('.modal-body').append(this.form.render().el);
        this.delegateEvents();
        return this;
      };
      RegisterModal.prototype.events = {
        'keyup': 'handleKey',
        'click .register': 'register',
        'click .cancel': 'cancel',
        'click .close': 'cancel'
      };
      RegisterModal.prototype.handleUsername = function() {
        return $.ajax({
          url: '/action/check-username',
          data: {
            username: this.form.getValue().username
          },
          success: this.usernameValidate,
          timeout: 500
        });
      };
      RegisterModal.prototype.usernameValidate = function(data) {
        if (data.exists === "true") {
          this.form.fields['username'].clearError();
          return this.form.fields['username'].setError("This username is taken");
        } else {
          return this.form.fields['username'].clearError();
        }
      };
      RegisterModal.prototype.handleEmail = function() {
        return $.ajax({
          url: '/action/check-email',
          data: {
            email: this.form.getValue().email
          },
          success: this.emailValidate,
          timeout: 500
        });
      };
      RegisterModal.prototype.emailValidate = function(data) {
        if (data.exists === "true") {
          this.form.fields['email'].clearError();
          return this.form.fields['email'].setError("This address is already registered");
        } else {
          return this.form.fields['email'].clearError();
        }
      };
      RegisterModal.prototype.enterPressed = function() {
        return this.register();
      };
      RegisterModal.prototype.register = function() {
        this.handleUsername();
        this.handleEmail();
        if (!this.form.validate()) {
          return $.post('/action/register', this.form.getValue(), this.serverValidate);
        }
      };
      RegisterModal.prototype.serverValidate = function(data) {
        if (data.result !== "success") {
          return this.form.fields['password2'].setError(data.message || "Unknown Error");
        } else {
          this.cancel();
          return Dialogs.modalMessage.showMessage({
            header: "Thank you",
            message: "<p>Thank you for registering, you should receive an e-mail confirming your registration shortly.</p>"
          });
        }
      };
      return RegisterModal;
    })();
    Dialogs.regModal = new RegisterModal();
    forgotSchema = {
      userid: {
        title: "User ID or E-mail",
        validators: ['required']
      }
    };
    ForgotModal = (function() {
      __extends(ForgotModal, ModalForm);
      function ForgotModal() {
        this.checkAccount = __bind(this.checkAccount, this);
        this.resetPassword = __bind(this.resetPassword, this);
        this.enterPressed = __bind(this.enterPressed, this);
        this.render = __bind(this.render, this);
        ForgotModal.__super__.constructor.apply(this, arguments);
      }
      ForgotModal.prototype.attributes = {
        id: 'forgotModal',
        "class": 'modal hide'
      };
      ForgotModal.prototype.initialize = function() {
        this.schema = forgotSchema;
        ForgotModal.__super__.initialize.call(this);
        return this;
      };
      ForgotModal.prototype.render = function() {
        this.$el.html(this.template({
          id: 'forgotModal',
          header: '<h1>Forgot your Password?</h1>',
          footer: '<a class="btn btn-primary reset-pw">Reset Password</a>\
                         <a class="btn cancel">Cancel</a>'
        }));
        this.$('.modal-body').append(this.form.render().el);
        this.delegateEvents();
        return this;
      };
      ForgotModal.prototype.enterPressed = function() {
        return this.resetPassword();
      };
      ForgotModal.prototype.events = {
        'keyup': 'handleKey',
        'click .reset-pw': 'resetPassword',
        'click .cancel': 'cancel',
        'click .close': 'cancel'
      };
      ForgotModal.prototype.resetPassword = function() {
        if (!this.form.validate()) {
          return $.post('/action/forgotpw', this.form.getValue(), this.checkAccount);
        }
      };
      ForgotModal.prototype.checkAccount = function(data) {
        if (data.result !== "success") {
          return this.form.fields['userid'].setError(data.message || "Unknown Error");
        } else {
          this.cancel();
          return Dialogs.modalMessage.showMessage({
            header: "Password Reset",
            message: "<p>Please check your e-mail for your temporary password</p>"
          });
        }
      };
      return ForgotModal;
    })();
    Dialogs.forgotModal = new ForgotModal();
    extractParams = function() {
      var params, qs, re, tokens;
      qs = document.location.search.split("+").join(" ");
      re = /[?&]?([^=]+)=([^&]*)/g;
      params = {};
      while (tokens = re.exec(qs)) {
        params[decodeURIComponent(tokens[1])] = decodeURIComponent(tokens[2]);
      }
      return params;
    };
    Dialogs.queryParams = extractParams();
    $(document).ready(function() {
      $('#homeCarousel').carousel({
        interval: $('#homeCarousel').length ? 10000 : void 0
      });
      $('.popover-link').popover({
        placement: 'bottom'
      });
      $('.login-button').bind('click', function(e) {
        e.preventDefault();
        return Dialogs.loginModal.show();
      });
      $('.register-button').bind('click', function(e) {
        e.preventDefault();
        return Dialogs.regModal.show();
      });
      $('#spinner').bind('ajaxSend', function() {
        return $(this).show();
      }).bind("ajaxStop", function() {
        return $(this).hide();
      }).bind("ajaxError", function() {
        return $(this).hide();
      });
      return $('.show-dform').bind('click', function(e) {
        var targ;
        e.preventDefault();
        targ = $(e.target);
        targ.siblings('.comment-form').show();
        return targ.hide();
      });
    });
    return Dialogs;
  });
}).call(this);
