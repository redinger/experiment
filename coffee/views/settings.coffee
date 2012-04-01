# Account Page and Tabs

define ['models/core', 'use!BackboneFormsBS', 'use!BackboneFormsEditors'],
  (Models) ->

    ## Account Settings Definitions
    accountSchemas =
     'personal':
           firstname:
                title: "First Name"
           lastname:
                title: "Last Name"
           bio:
                title: "Bio"
                type: "TextArea"
                editorAttrs:
                        maxlength: 256
                        rows: 4
                        style: "resize: none;"
           gender:
                type: 'Select'
                options: [
                        {
                         val: 'female'
                         label: 'Female'
                        }, {
                         val: 'male'
                         label: 'Male'
                        } ]
           dob:
                title: "Date of Birth"
                type: "Date"
           email:
                title: "Email"
                validators: ["required", "email"]
                dataType: 'email'
           cell:
                title: "Cell Phone"
                help: "Used to send SMS messages when requested"
                dataType: 'phn'
     'password':
           oldPass:
                title: "Old Password"
                type: 'Password'
                validators: ['required']
           newPass1:
                title: "New Password"
                type: 'Password'
                validators: ['required']
           newPass2:
                title: "Confirm Password"
                type: 'Password'
                validators: ['required',
                     type: 'match'
                     field: 'newPass1'
                     message: 'Passwords must match'
                ]

    accountFieldsets =
      'personal':
        [{
              legend: 'Personal'
              fields: ['firstname', 'lastname', 'bio', 'gender', 'dob', 'email']
        }, {
              legend: 'Services'
              fields: ['cell']
        }]

    accountTitles =
      'personal': "Personal Preferences"
      'password': "Change Password"

    ## Generic Settings View
    class AccountView extends Backbone.View
      initialize: () ->
        @pane = @options.pane
        @form = new Backbone.Form
                schema: accountSchemas[@pane]
                fieldsets: accountFieldsets[@pane]
                data: @options.data or {}

      render: () =>
        heading = '<h1>' + accountTitles[@pane] + '</h1>'
        @$el.append heading
        @$el.append @form.render().el
        if @pane is 'password'
           @$el.append "<button class='btn pwcommit'>Update</button>"
        @delegateEvents()
        @

      events:
        'change [type=checkbox]': 'handleCheckBox'
        'keyup input': 'handleKeyup'
        'focus input': 'handleFocus'
        'click .pwcommit': 'handlePasswordUpdate'

      handleCheckBox: (event) =>
        alert 'checkbox'

      handleChange: (event) =>
        if not @form.validate()
           @updateUser @form.getValue()

      handleKeyup: (event) =>
        if event.which == 13
           event.preventDefault()
           if @pane is 'password'
              @handlePasswordUpdate()
           else
              @handleChange
        else
           @form.validate()

      handlePasswordUpdate: (event) =>
        if not @form.validate()
           $.ajax
                url: '/action/changepw'
                type: 'POST'
                data: @form.getValue()
                success: @passwordValidate
                error: @passwordValidateError
                timeout: 5000

      passwordValidate: (data) =>
        if data.result is "success"
           @form.fields['oldPass'].clearError()
           (@form.fields[key].setValue("") for own key, val of @form.getValue())
           window.PE.modalMessage.showMessage
                header: "Password Set"
                message: "Your password was set to the new value"
        if data.result is "fail"
           @form.fields['oldPass'].clearError()
           @form.fields['oldPass'].setError data.message

      passwordValidateError: (data, status, error) =>
        console.log data
        console.log status
        console.log error

      handleFocus: (event) =>

      updateUser: (values) ->


    ## Account Page Setup
    renderSettings = (pane) ->
      view = new AccountView
                el: $('.accountSettings')
                pane: pane
                data: {}
      view.render()

    renderServices = () ->
      view = new ServicesView
                el: $('#services')
                data: {}
      view.render()

    $(document).ready ->
        if $('.accountSettings')
           renderSettings $('.accountSettings').attr('id')
        if $('.serviceSettings')
           renderServices

