define ['models/infra', 'use!BackboneFormsBS', 'use!BackboneFormsEditors'],
  (Infra) ->

# ## User Preferences Schemas

    schemaUserPrefs =
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
                      label: 'Male' } ]
        dob:
           title: "Date of Birth"
           type: "Date"
        cell:
           title: "Cell Phone"
           help: "Used to send SMS messages when requested"
           dataType: 'phone'

    # ## Embedded User Preferences
    class UserPrefs extends Infra.Model
      serverType: 'userprefs'
      schema: schemaUserPrefs

    # ## Export models

    UserPrefs: UserPrefs
