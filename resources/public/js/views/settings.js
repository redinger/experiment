(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; }, __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype;
    child.prototype = new ctor;
    child.__super__ = parent.prototype;
    return child;
  };
  define(['models/core', 'use!BackboneFormsBS', 'use!BackboneFormsEditors'], function(Models) {
    var AccountView, accountFieldsets, accountSchemas, accountTitles, renderServices, renderSettings;
    accountSchemas = {
      'personal': {
        firstname: {
          title: "First Name"
        },
        lastname: {
          title: "Last Name"
        },
        bio: {
          title: "Bio",
          type: "TextArea",
          editorAttrs: {
            maxlength: 256,
            rows: 4,
            style: "resize: none;"
          }
        },
        gender: {
          type: 'Select',
          options: [
            {
              val: 'female',
              label: 'Female'
            }, {
              val: 'male',
              label: 'Male'
            }
          ]
        },
        dob: {
          title: "Date of Birth",
          type: "Date"
        },
        email: {
          title: "Email",
          validators: ["required", "email"],
          dataType: 'email'
        },
        cell: {
          title: "Cell Phone",
          help: "Used to send SMS messages when requested",
          dataType: 'phn'
        }
      },
      'password': {
        oldPass: {
          title: "Old Password",
          type: 'Password',
          validators: ['required']
        },
        newPass1: {
          title: "New Password",
          type: 'Password',
          validators: ['required']
        },
        newPass2: {
          title: "Confirm Password",
          type: 'Password',
          validators: [
            'required', {
              type: 'match',
              field: 'newPass1',
              message: 'Passwords must match'
            }
          ]
        }
      }
    };
    accountFieldsets = {
      'personal': [
        {
          legend: 'Personal',
          fields: ['firstname', 'lastname', 'bio', 'gender', 'dob', 'email']
        }, {
          legend: 'Services',
          fields: ['cell']
        }
      ]
    };
    accountTitles = {
      'personal': "Personal Preferences",
      'password': "Change Password"
    };
    AccountView = (function() {
      __extends(AccountView, Backbone.View);
      function AccountView() {
        this.handleFocus = __bind(this.handleFocus, this);
        this.passwordValidateError = __bind(this.passwordValidateError, this);
        this.passwordValidate = __bind(this.passwordValidate, this);
        this.handlePasswordUpdate = __bind(this.handlePasswordUpdate, this);
        this.handleKeyup = __bind(this.handleKeyup, this);
        this.handleChange = __bind(this.handleChange, this);
        this.handleCheckBox = __bind(this.handleCheckBox, this);
        this.render = __bind(this.render, this);
        AccountView.__super__.constructor.apply(this, arguments);
      }
      AccountView.prototype.initialize = function() {
        this.pane = this.options.pane;
        return this.form = new Backbone.Form({
          schema: accountSchemas[this.pane],
          fieldsets: accountFieldsets[this.pane],
          data: this.options.data || {}
        });
      };
      AccountView.prototype.render = function() {
        var heading;
        heading = '<h1>' + accountTitles[this.pane] + '</h1>';
        this.$el.append(heading);
        this.$el.append(this.form.render().el);
        if (this.pane === 'password') {
          this.$el.append("<button class='btn pwcommit'>Update</button>");
        }
        this.delegateEvents();
        return this;
      };
      AccountView.prototype.events = {
        'change [type=checkbox]': 'handleCheckBox',
        'keyup input': 'handleKeyup',
        'focus input': 'handleFocus',
        'click .pwcommit': 'handlePasswordUpdate'
      };
      AccountView.prototype.handleCheckBox = function(event) {
        return alert('checkbox');
      };
      AccountView.prototype.handleChange = function(event) {
        if (!this.form.validate()) {
          return this.updateUser(this.form.getValue());
        }
      };
      AccountView.prototype.handleKeyup = function(event) {
        if (event.which === 13) {
          event.preventDefault();
          if (this.pane === 'password') {
            return this.handlePasswordUpdate();
          } else {
            return this.handleChange;
          }
        } else {
          return this.form.validate();
        }
      };
      AccountView.prototype.handlePasswordUpdate = function(event) {
        if (!this.form.validate()) {
          return $.ajax({
            url: '/action/changepw',
            type: 'POST',
            data: this.form.getValue(),
            success: this.passwordValidate,
            error: this.passwordValidateError,
            timeout: 5000
          });
        }
      };
      AccountView.prototype.passwordValidate = function(data) {
        var key, val, _ref;
        if (data.result === "success") {
          this.form.fields['oldPass'].clearError();
          _ref = this.form.getValue();
          for (key in _ref) {
            if (!__hasProp.call(_ref, key)) continue;
            val = _ref[key];
            this.form.fields[key].setValue("");
          }
          window.PE.modalMessage.showMessage({
            header: "Password Set",
            message: "Your password was set to the new value"
          });
        }
        if (data.result === "fail") {
          this.form.fields['oldPass'].clearError();
          return this.form.fields['oldPass'].setError(data.message);
        }
      };
      AccountView.prototype.passwordValidateError = function(data, status, error) {
        console.log(data);
        console.log(status);
        return console.log(error);
      };
      AccountView.prototype.handleFocus = function(event) {};
      AccountView.prototype.updateUser = function(values) {};
      return AccountView;
    })();
    renderSettings = function(pane) {
      var view;
      view = new AccountView({
        el: $('.accountSettings'),
        pane: pane,
        data: {}
      });
      return view.render();
    };
    renderServices = function() {
      var view;
      view = new ServicesView({
        el: $('#services'),
        data: {}
      });
      return view.render();
    };
    return $(document).ready(function() {
      if ($('.accountSettings')) {
        renderSettings($('.accountSettings').attr('id'));
      }
      if ($('.serviceSettings')) {
        return renderServices;
      }
    });
  });
}).call(this);
