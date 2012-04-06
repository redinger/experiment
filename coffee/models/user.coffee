define ['models/core', 'use!BackboneFormsBS', 'use!BackboneFormsEditors'],
  (Models) ->

# ## Preferences Schemas

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

# ## Embedded User Models

    class UserPrefs extends Common.Model
      schema: accountSchemas['personal']



    models =
      UserPrefsPersonal: UserPrefs

    Backbone.ReferenceCache.registerTypes Models.cacheTypes(models)
