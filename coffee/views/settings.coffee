# Account Page and Tabs

define ['models/infra', 'models/core', 'views/widgets', 'use!Bootstrap', 'use!BackboneFormsBS', 'use!BackboneFormsEditors'],
  (Infra, Core, Widgets) ->

    # ## View for updating User Preferences Submodel

    class PreferencesView extends Backbone.View
      id: "preferences"
      class: "tab-pane"
      initialize: () ->
        @model = Core.theUser.preferences
        @form = new Backbone.Form
                model: @model

      render: () =>
        heading = "<div class='page-header'><h1>Preferences</h1></div>"
        @$el.append heading
        @$el.append @form.render().el
        @

      updateModel: () =>
        errors = @form.commit()
        if errors
          console.log errors
        else
          @model.save()

      events:
        'keyup': 'handleKeyUp'
        'change [type=checkbox]': 'handleCheckBox'
        'change [type!=checkbox]': 'updateModel'

      handleKeyUp: (event) =>
        if not @handleKeyUpAux
          @handleKeyUpAux = _.debounce () =>
            @updateModel()
          , 1000
        @handleKeyUpAux()

      handleCheckBox: (event) =>
        alert 'checkbox'


    # # Change Password --

    schemaPassword =
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

    class ChangePasswordView extends Backbone.View
      id: "password"
      className: "tab-pane"
      initialize: ->
        @form = new Backbone.Form
                  schema: schemaPassword
                  data: {}
        @

      render: ->
        @$el.append "<div class='page-header'><h1>Change Password</h1></div>"
        @$el.append @form.render().el
        @$el.append "<button class='btn pwcommit'>Update</button>"
        @

      events:
        'click .pwcommit': 'handlePasswordUpdate'
        'keyup input': 'handleKeyup'

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



    # # Services View
    # ---------------------------------------------------------
    # Provide a view for adding/removing service configurations,
    # sort of like Mint.com's accounts page


    # ## Service Sub-view
    #
    # Takes a configuration option (from the registry)
    # Generates a form from it
    #
    class ServiceView extends Backbone.View
      className: "service-view"
      initialize: (options) ->
        @rendered = false
        @config = options.config
        if @config? and @config.oauth? and @config.oauth
           @template = Infra.templateLoader.getTemplate "service-oauth-template"
        else
           @template = Infra.templateLoader.getTemplate "service-template"
           @form = new Backbone.Form
             model: @model
             schema: @config.schema
             fields: @config.fields
        @

      # Rendering the View
      render: ->
        if not @rendered
          if @config? and @config.oauth?
             @renderOauth()
          else
             @renderForm()
        @rendered = true
        @

      renderOauth: ->
        data = @model.toJSON()
        data.config = @config
        @$el.html @template data
        @

      renderForm: ->
        data = @model.toJSON()
        data.config = @config
        @$el.html @template data
        @$('.svcform').append @form.render().el
        @

      # Handling UI Events
      events:
        'keyup input[type=text]': 'handleKey'
        'keyup input[type=password]': 'handleKey'
        'change input': 'update'

      handleKey: =>
        if not @handleKeyAux
           @handleKeyAux = _.debounce @update, 1000
        @handleKeyAux()

      update: =>
        error = @form.commit()
        if not error
           @model.save {}, {silent: true}


    # ## Service submodel containers
    class ServicesView extends Backbone.View
      id: "services"
      className: "tab-pane"
      initialize: (options) ->
        # Registry is 'tag: {schema:, fields:, ...}' (move to model?)
        @registry = $.parseJSON $('#services-registry').html()
        if _.keys(@registry) < 1
          alert 'Registry was not rendered for ServicesView'
        else
          console.log @registry

        # Manage our subviews
        @views = []
        @collection.each @addSubView, @

        # Template for header dropdown and body entries
        @template = Infra.templateLoader.getTemplate "services-header-template"

        # Compute the list of possible new services
        @computeServiceList()

        # In case services change in the background
        @collection.on 'add remove', ->
           @render()
        , @
        @

      addSubView: (model) ->
        config = @registry[model.id]
        if config?
          view = new ServiceView
            model: model
            config: @registry[model.id]
          @views = [view].concat @views

      # NOTE: Move to services model
      computeServiceList: () ->
        records = _.clone @registry
        # Make the tag available to config consumers
        _.each records, (record, tag) ->
          record.tag = tag
        # Remove those configs already registered
        @collection.each (model) ->
          delete records[model.id]
        # Records not configured
        options = _.values(records)
        @serviceList = options

      events:
        'click .new': 'newService'
        'click .del': 'delService'

      newService: (event) =>
        # What view?
        event.preventDefault()
        tag = $(event.currentTarget).attr('href')
        # Create and add the view
        model = @collection.create
          id: tag
          type: "service"
        @collection.add model if model
        @addSubView model
        # Update page
        @render()

      delService: (event) =>
        # Get the view/model
        event.preventDefault()
        tag = $(event.currentTarget).attr('data-tag')
        view = _.find @views, (view) ->
           view.config.tag is tag
        # Remove view
        @views = _.without @views, view
        view.model.destroy()
        # Update page
        @render()

      render: ->
        @computeServiceList()
        @$el.html @template
          services: @serviceList
        _.each @views, (view) =>
          @$el.append view.render().el
        , @
        @


    # Page Application Harness
    # -----------------------------------------------------
    class SettingsRouter extends Backbone.Router
      initialize: (options = {}) ->
          @default = options.default
          @

      routes:
          ':tab/*path': 'selectTab'
          ':tab': 'selectTab'

    class Settings extends Backbone.View
      className: "tab-content"
      initialize: ->
        @router = new SettingsRouter()
        @navbar = new Widgets.NavBar
          el: $('.subnav-fixed-top')
          router: @router

        @views = {}
        @views.preferences = new PreferencesView()
        @views.password = new ChangePasswordView()
        @views.services = new ServicesView
          collection: Core.theUser.services
        @

      render: ->
        _.each @views, (view) ->
          @$el.append view.render().el
        , @
        @


    # # Start Settings Page App
    $(document).ready ->
      settings = new Settings()
      $('div#main').append settings.render().el

      Backbone.history.start
        root: '/account/'
        pushState: true
