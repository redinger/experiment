define ['models/infra', 'models/core', 'models/user', 'views/widgets', 'views/journal', 'views/scheduling', 'views/common', 'use!Moment', 'use!Handlebars', 'use!BackboneFormsBS', 'use!BackboneFormsEditors'],
  (Infra, Core, User, Widgets, Journal, Scheduling, Common, moment) ->

# ## Related Objects -
#
# A standard view for viewing a list of objects connected to the current
# object in some way.  Views results of an ajax server call based on the
# current model

    class RelatedObjects extends Backbone.View
      attributes:
        class: "related-objects"

      initialize: ->
        @templates = {}
        @templates.treatment = Infra.templateLoader.getTemplate 'treatment-row-view'
        @templates.instrument = Infra.templateLoader.getTemplate 'instrument-row-view'
        @templates.experiment = Infra.templateLoader.getTemplate 'experiment-row-view'
        @total = 0
        @results = null
        @fetchRelated()
        @

      fetchRelated: ->
        $.ajax "/api/search/related/#{@model.get('type')}/#{@model.id}",
          context: @
          success: (results) =>
            @total = results.hits
            @results = _.map results.models, (result) ->
              model = Backbone.ReferenceCache.resolve null, null,
                attrs: result
              model.watch 'change', @render, @
              model
            , @
            @render()

      render: ->
        @$el.html '<h3>Related</h3>'
        _.map @results, (result) ->
          @$el.append @templates[result.type()] result.toTemplateJSON()
        , @
        @

      events:
        'click a': 'view'

      view: ->
        event.preventDefault()
        id = $(event.target).attr('data-id')
        type = $(event.target).attr('data-type')
        model = Backbone.ReferenceCache.resolve type, id
        @trigger 'nav:view', model if model?

# ### Protocol for adding/removing an explorer view from the main DOM
    class ExplorerSubview extends Backbone.View
      attach: (fn, parent) ->
        @on 'all', fn, parent
        @delegateEvents()

      detach: (fn, parent) ->
        @undelegateEvents()
        @off 'all', fn, parent
        @remove()

# Item Views
# --------------------------------------------

# Base class provides common init, handlers and rendering
# Subclasses customize the template, listening, etc.
# Subclasses must also specify all events that need to be
# handled.
#
# The ItemView triggers inter-view actions via the 'nav:...'
# event namespace
#
    class ItemView extends ExplorerSubview
      initialize: ->
        @model.watch 'change', @render, @
        @related = new RelatedObjects
            model: @model
        @related.on 'all', @forwardEvents, @
#        @discuss = new Discussion
#            model: @model
        @

      forwardEvents: (action, arg1, arg2) ->
          @trigger action, arg1, arg2

      attach: (fn, parent) ->
          super fn, parent
          @related.delegateEvents()

      detach: (fn, parent) ->
          super fn, parent
          @related.undelegateEvents()
          @related.remove()

      render: ->
        if @model.isLoaded()
           @$el.html @template @model.toTemplateJSON()
           @$('#related').html @related.render().el
           @renderAux() if @renderAux?
        else if not @model._loading
           @model.fetch()
        @

      # Common Event Handlers
      addTags: (event) =>
        event.preventDefault()
        if not @tagDialog?
          @tagDialog = new Widgets.AddTagDialog()
          @tagDialog.on 'form:accept', (result) ->
             @model.addTagString result.tags
          , @
        @tagDialog.show()

      view: =>
        event.preventDefault()
        id = $(event.target).attr('data-id')
        type = $(event.target).attr('data-type')
        model = Backbone.ReferenceCache.resolve type, id
        @trigger 'nav:view', model

      edit: =>
        @trigger 'nav:edit', @model

      clone: =>
        clone = @model.cloneModel()
        @trigger 'nav:create', clone, true


#
# Experiments
#
    class ExperimentView extends ItemView
      initialize: (options) ->
        super(options)
        @template = Infra.templateLoader.getTemplate 'experiment-view'
        @

      events:
        'click a.view': 'view'
        'click .edit': 'edit'
        'click .clone': 'clone'
        'click .add-tag': 'addTags'
        'click .run': 'startTrial'

      startTrial: =>
        trial = new (Backbone.ReferenceCache.lookupConstructor('trial'))
        trial.setDefaults()
        trial.set('experiment', @model)
        @trigger 'nav:configTrial', trial

#
# Treatments
#
# Experiments (membership), Instruments (via tags)
#
    class TreatmentView extends ItemView
      initialize: (options) ->
        super(options)
        @template = Infra.templateLoader.getTemplate 'treatment-view'
        @jview = new Journal.View
            model: @model
        @

      renderAux: ->
        @$('#journal').html @jview.render().el
        @

      events:
        'click .add-tag': 'addTags'
        'click .edit': 'edit'
        'click .clone': 'clone'

#
# Instruments
#
# Experiments (membership), Treatments (via tags)
#
    class InstrumentView extends ItemView
      initialize: (options) ->
        super(options)
        @template = Infra.templateLoader.getTemplate 'instrument-view'
        @

      events:
        'click .edit': 'edit'
        'click .add-tag': 'addTags'
        'click .track': 'track'
        'click .untrack': 'untrack'

      track: (event) =>
        Scheduling.configureTrackerSchedule @, @model, @trackSchedule

      trackSchedule: (schedule) =>
        @model.track schedule

      untrack: (event) =>
        Common.modalMessage.showMessage
          header: "Stop tracking '#{@model.title()}'"
          message: "<p>Are you sure you wish to stop tracking?  You may lose historical data by disabling this tracker.</p>"
          accept: "Yes"
          reject: "No"
          callback: (result) =>
            @model.untrack() if result is 'accept'


# Item Creators
# --------------------------------------------

    treatmentFieldsets = [
      legend: "Content"
      fields: ['name', 'description', 'reminder']
    ,
      legend: "Treatment Information"
      fields: ['dynamics']
    ]

    # ## Treatment
    class TreatmentEdit extends ExplorerSubview
      initialize: (options) ->
        alert('Treatment editor requires treatment object, got ' + @model.get('type')) if @model.get('type') is not 'treatment'
        @model.watch 'change', @render, @
        @template = Infra.templateLoader.getTemplate 'treatment-editor'
        @form = new Backbone.Form
            model: @model
            fieldsets: treatmentFieldsets
        @

      render: ->
        @$el.html @template @model.toTemplateJSON()
        @$('#editForm').html @form.render().el
        @

      events:
        'click .accept': 'accept'
        'click .cancel': 'cancel'

      accept: =>
        errors = @form.commit();
        if not errors
           @model.save {},
             wait: true
             success: (model, resp) =>
               @trigger 'nav:view', model
             failure: (model, resp) =>
               alert 'Server Error Saving Model'

      cancel: =>
        if @model.name()? and @model.id?
          @trigger 'nav:view', @model
        else
          @trigger 'nav:search'


    # ## Instrument
    class InstrumentEdit extends ExplorerSubview
      initialize: (options) ->
        alert('Treatment editor requires treatment object, got ' + @model.get('type')) if @model.get('type') is not 'treatment'
        @model.watch 'change', @render, @
        @template = Infra.templateLoader.getTemplate 'instrument-editor'
        @form = new Backbone.Form
            model: @model
        @

      render: ->
        @$el.html @template @model.toTemplateJSON()
        @$('#editForm').html @form.render().el
        @

      events:
        'click .cancel': 'cancel'
        'click .accept': 'accept'

      cancel: =>
        if @model.name()?
          @trigger 'nav:view', @model
        else
          @trigger 'nav:search'

      accept: =>
        errors = @form.commit();
        if not errors
           @model.save {},
             wait: true
             success: (model, resp) =>
               @trigger 'nav:view', model
             failure: (model, resp) =>
               alert 'Server Error Saving Model'

    # ## EXPERIMENT
    class ExperimentEdit extends ExplorerSubview
      initialize: (options) ->
        @template = Infra.templateLoader.getTemplate 'experiment-editor'
        @

      render: ->
        if @model.isLoaded()
           @$el.html @template @model.toTemplateJSON()
        else if not @model._loading?
           @model.fetch()
        @


    # ## TRIAL CONFIGURATION
    class ConfigureTrackerView extends Backbone.View
      initialize: (options) ->
        @parent = options.parent
        @

      trackedp: ->
        _title = @model.title()
        Core.theUser.trackers.find (tracker) ->
            if tracker.instrument.title() is _title then true else false

      instruments: ->
        Core.theUser.trackers.map (tracker) ->
           tracker.instrument

      validate: ->
        if @trackedp()
          @$('.tracker-error').remove()
          true
        else
          existing = @$('.tracker-error')
          console.log existing.length
          console.log _.isEmpty existing
          if  existing.length is 0
            @$el.prepend "<p class='tracker-error' style='color:red;'>You must configure tracking to start the trial</p>"
          false

      render: ->
        console.log 'rendering tracker configurator'
        status = if @trackedp() then '<b>Enabled</b>' else "<a href='#' class='configure'>Configure</a>"
        @$el.html "<p> <span class='tracker-title'>#{ @model.title() }</span> <span class='tracker-status'>#{ status }</span></p>"
        @

      events:
        'click .configure': 'configure'

      configure: (e) =>
        e.preventDefault()
        Scheduling.configureTrackerSchedule @parent, @model, @trackSchedule

      trackSchedule: (schedule) =>
        @model.track schedule

    class ConfigureTrialView extends ExplorerSubview
      initialize: (options) ->
        @template = Infra.templateLoader.getTemplate 'configure-trial-view'
        exp = @model.experiment
        outcomeViews = exp.outcome.map @createTracker, @
        covariateViews = exp.covariates.map @createTracker, @
        @trackerViews = outcomeViews.concat covariateViews
        _.each @trackerViews, (view) ->
            view.on 'all', @forwardEvents, @
        , @
        @form = new Backbone.Form
          model: @model
        @

      forwardEvents: (action, arg1, arg2) ->
          @trigger action, arg1, arg2

      createTracker: (inst) ->
        new ConfigureTrackerView
          model: inst
          parent: @

      renderTrackers: () ->
        target = @$('#configureTrackers')
        target.html '<h3>Track Measurements</h3>'
        _.each @trackerViews, (view) ->
           target.append view.render().el
        @

      validateTrackers: () ->
        _.all @trackerViews, (view) ->
            view.validate()

      render: ->
        @$el.html @template @model.toTemplateJSON()
        @$('#configureForm').html @form.render().el
        @$('.bbf-date input').datepicker 'option', 'dateFormat', 'mm/dd/yy'
        @$('.bbf-date input').datepicker 'refresh'
        @renderTrackers()
        @

      events:
        'click .accept': 'create'
        'click .cancel': 'cancel'

      create: =>
        event.preventDefault()
        errors = @form.commit()
        if not errors and @validateTrackers()
          Core.theUser.trials.create @model.toJSON(),
            wait: true
            success: (model, resp) =>
              window.location = '/dashboard/overview'
            failure: (model, resp) =>
              alert 'Server error creating model'

      cancel: =>
        event.preventDefault()
        if @model.experiment? and @model.experiment.name()
           @trigger 'nav:view', @model.experiment
        else
           @trigger 'nav:search'

# Search results and state
# --------------------------------------------

    # Search results model
    class SearchResults extends Backbone.Collection
      initialize: (options) ->
        @total = 0
        @query = ""
        @page = 1
        @pages = 1
        if options? and options.pagesize?
           @size = options.pagesize
        else
           @size = 5
        @

      reset: (results, options) ->
        models = _.map results, (object) ->
           Backbone.ReferenceCache.import object
        super models, options
        @

      doQuery: (query, page) ->
        @last = 'query'
        @query = query or @query
        if @query.length is 0
           @query = 'show treatments'
        @page = page or @page or 1
        offset = (@page - 1) * @size
        $.ajax "/api/search/query/#{@query}/#{@size}/#{offset}",
          context: @
          success: (results) =>
            @total = results.hits
            @pages = Math.ceil(@total / @size)
            @trigger 'search:pagecount', @pages
            @reset results.models

      doTagQuery: (tag, page) ->
        @last = 'tag'
        @query = '"' + ( tag or @query ) + '"'
        @page = page or @page or 1
        offset = (@page - 1) * @size
        $.ajax "/api/search/tag/#{@query}/#{@size}/#{offset}",
          context: @
          success: (results) =>
            @total = results.hits
            @pages = Math.ceil(@total / @size)
            @trigger 'search:pagecount', @pages
            @reset results.models

    # Single result view, multiple types
    class ResultView extends Backbone.View
      attributes:
        class: 'search-result'

      initialize: (options) ->
        @template = Infra.templateLoader.getTemplate switch @model.get('type')
              when "experiment" then 'experiment-list-view'
              when "instrument" then 'instrument-list-view'
              when "treatment" then 'treatment-list-view'
        @model = Backbone.ReferenceCache.resolve options.type, options.mid if not @model?
        @model.watch 'change', @render, @
        @

      render: ->
        if @model? and @model.isLoaded()
           @$el.html @template @model.toTemplateJSON()
           @$el.append '<hr>'
        @

      events: ->
        'click a': 'view'

      view: (event) =>
        event.preventDefault()
        @trigger 'nav:view', @model

    # Result page and handlers
    class SearchPage extends ExplorerSubview
      # STATE
      initialize: (options) ->
        # View maintenance and rendering
        @template = Infra.templateLoader.getTemplate 'search-header'
        @popular = Infra.templateLoader.getTemplate 'popular-searches'
        @pagination = new Widgets.Pagination()
        @pagination.on 'pagination:change', (page) ->
          @trigger 'pagination:change', page
        , @
        @views = []

        # Result objects
        @results = options.results
        @results.on 'reset', @updateViews
        @render()
        @


      # MODEL EVENTS
      updateViews: () =>
        # Update header views
        @$('.search-query').val([@results.query])
        @pagination.changePage @results.page, @results.pages
        # Render our results
        @renderResults()

      # RENDERING
      render: ->
        # Render or update search bar
        if @$('.search-query').length == 0
           @$el.append @template @query
           @$('#pagination').append @pagination.render().el
        @

      attach: (fn, parent) ->
          super fn, parent
          @pagination.delegateEvents()
          _.each @views, (view) ->
            view.delegateEvents()

      detach: (fn) ->
          super fn
          @pagination.undelegateEvents()

      renderResults: ->
        # Remove if exist
        _.each @views, (view) ->
           view.remove()
        # Create new
        @views = @results.map (model) ->
           new ResultView
             model: model
        # Render new
        _.each @views, (view) =>
           view.on 'all', (action, model) ->
             @trigger action, model
           , @
           @$('#results').append view.render().el
        @

      # UI EVENTS
      events:
        'keyup input[type=text]': 'handleKey'
        'click .search-btn': 'submitQuery'
        'click .help-btn': 'showHelp'
        'click .popsearch': 'doSearch'

      doSearch: (event) =>
        event.preventDefault()
        query = $(event.target).text()
        @trigger 'nav:search', query

      handleKey: (event) =>
        if event.which is 13
           @submitQuery(event)
           false
        else
           true

      submitQuery: (event) =>
        event.preventDefault()
        query = @$('input').val() or ""
        @trigger 'nav:search', query
        true

      showHelp: (event) =>
        Common.modalMessage.showMessage
          header: "Search Help"
          message: "To see different types of objects: 'show <type>' where type can be instrument, treatment, or experiment.<br>
      To filter by a given treatment: 'using <treatment keyword>'"



# Explorer Router
# -------------------------------------------
    class ExploreRouter extends Backbone.Router
      initialize: (options = {}) ->
          @default = options.default
          @

      routes:
          '': 'selectDefault'
          'search/query': 'searchQuery'
          'search/query/:query': 'searchQuery'
          'search/query/:query/p:page': 'searchQuery'
          'search/tag/:tag': 'searchTag'
          'search/tag/:tag/p:page': 'searchTag'
          'view/:type/:id': 'viewObject'
          'edit/:type/:id': 'editObject'
          'create/:type': 'createObject'
          '*path': 'selectDefault'

      selectDefault: (data) =>
          @navigate @default, {trigger: true, replace: true} if @default?

      navQuery: (query, page) ->
          if query
            @navigate "/search/query/#{query}/p#{page}",
              trigger: true
          else
            @navigate "/search/query",
              trigger: true

      navTag: (tag, page) ->
          @navigate "/search/tag/#{tag}/p#{page}",
            trigger: true

      navEditModel: (model, trigger) ->
          @navigate "/edit/#{model.get('type')}/#{model.id}",
            trigger: if trigger? then trigger else true

      navCreateModel: (type, trigger) ->
          @navigate "/create/#{type}",
            trigger: if trigger? then trigger else true

      navViewModel: (model) ->
          @navigate "/view/#{model.get('type')}/#{model.id}",
            trigger: true


# Explorer Breadcrumbs
# -------------------------------------------

    class Breadcrumbs extends Backbone.View
      initialize: (options = {}) ->
        @template = Infra.templateLoader.getTemplate 'breadcrumbs-view'
        @base =
          name: "Explore"
          class: ""
          url: "/"
        @crumbs =
          path: []
          tail: @base
        @router = options.router or null
        @router.on 'route:viewObject', @viewCrumbs
        @router.on 'route:editObject', @editCrumbs
        @router.on 'route:createObject', @editCrumbs
        @router.on 'route:searchQuery', @queryCrumbs
        @router.on 'route:searchTag', @tagCrumbs
        @

      viewCrumbs: (type, id) =>
        @model.off 'change', @rerenderObj, @ if @model?
        @model = Backbone.ReferenceCache.resolve(type, id)
        @model.on 'change', @rerenderObj, @
        view =
            name: "View"
            class: ""
            url: "/view"
        thetype =
            name: type[0].toUpperCase() + type.slice(1)
            class: ""
            url: "/view/#{type.toLowerCase()}"
        @crumbs.path = [@base, view, thetype]
        @crumbs.tail =
            name: @model.name()
            class: "active"
            url: "/view/#{type}/#{id}"
        @render()

      editCrumbs: (type, id) =>
        @model.off 'change', @rerenderObj, @ if @model?
        if type? and id?
          @model = Backbone.ReferenceCache.resolve(type, id)
          @model.on 'change', @rerenderObj, @
        else
          @model = null
        view =
            name: "Edit"
            class: ""
            url: "/edit"
        thetype =
            name: type[0].toUpperCase() + type.slice(1)
            class: ""
            url: "/edit/#{type.toLowerCase()}"
        @crumbs =
           path: [@base, view, thetype]
           tail:
             name: if @model? then @model.name() else "Create"
             class: "active"
             url: "/edit/#{type}/#{id}"
        @render()

      queryCrumbs: (query, page) =>
        @crumbs.path = [ @base,
            name: "Query"
            class: ""
            url: "/search/query"
        ]
        @crumbs.tail =
           name: query
           class: "active"
           url: if page then "/search/query/#{query}/#{page}" else "/search/query/#{query}"
        @render()

      tagCrumbs: (tag) =>
        @crumbs =
          path: [@base,
            name: "Tags"
            class: ""
            url: "/search/tag"
          ]
          tail:
            name: tag
            class: "active"
            url: "#"
        @render()

      rerenderObj: ->
        @crumbs.tail.name = @model.name()
        @render()

      render: ->
        @$el.html @template @crumbs
        @

      events:
        'click a': 'navigate'

      navigate: (event) =>
        event.preventDefault()
        link = $(event.currentTarget).attr('href')
        @router.navigate link, { trigger: true, replace: true } if @router


# Explorer Application
# -------------------------------------------

    class ExploreHome extends Backbone.View
      initialize: ->
        # Manage some views, default is search
        @views = {}
        @editors = {}
        @results = new SearchResults()
        @search = new SearchPage
          results: @results
        @search.on 'pagination:change', @changeSearchPage, @
        @search.on 'all', @viewAction, @
        @currentView = @search

        # Change views based on router events
        @router = new ExploreRouter
          default: 'search/query'
        @router.on 'route:viewObject', @viewObject
        @router.on 'route:editObject', @editObject
        @router.on 'route:createObject', @createObject
        @router.on 'route:startTrial', @startTrial
        @router.on 'route:searchQuery', @doSearch
        @router.on 'route:searchTag', @doTagSearch
        @

      render: () ->
        @$el.append @currentView.render().el if @currentView
        @

      # Common events
      events:
        'click .tags .label': 'searchTagClick'
        'click .create-treatment': 'createTreat'
        'click .create-instrument': 'createInst'
        'click .create-experiment': 'createExp'

      # Common UI event handlers
      searchTagClick: (event) =>
        event.preventDefault()
        tag = $(event.currentTarget).text()
        @router.navTag tag, 1

      createExp: (event) =>
        event.preventDefault()
        @router.navCreateModel('experiment')

      createTreat: (event) =>
        event.preventDefault()
        @router.navCreateModel('treatment')

      createInst: (event) =>
        event.preventDefault()
        @router.navCreateModel('instrument')

      # Managing view and view navigation actions
      changeView: (view) ->
        @currentView.detach(@viewAction, @) if @currentView?
        @currentView = view
        @currentView.attach(@viewAction, @)
        @render()

      viewAction: (action, object, arg) ->
        if action is 'nav:edit'
            if arg # arg == direct; if true implies direct view (no URL change)
               editor = @createEditView object
               @changeView editor
               @router.navEditModel object, false
            else
               @router.navEditModel object
        else if action is 'nav:create'
            if arg
               editor = @createEditView object
               @changeView editor
               @router.navCreateModel object.type(), false
            else
               @router.navCreateModel object
        else if action is 'nav:view'
            @router.navViewModel object
        else if action is 'nav:doView'
            @changeView object
        else if action is 'nav:configTrial'
            editor = new ConfigureTrialView
                model: object
            @changeView editor
            @router.navCreateModel 'trial', false
        else if action is 'nav:search'
            if object? and object.length > 0
               @router.navQuery(object, arg or 1)
            else
               @router.navQuery(null)

      changeSearchPage: (page) ->
        @viewAction 'nav:search', @results.query, page

      # Router View Object Handler
      viewObject: (type,id) =>
        if not @views[id]?
           model = Backbone.ReferenceCache.resolve type, id
           console.log model
           switch type
             when "experiment" then view = new ExperimentView
                model: model
             when "treatment" then view = new TreatmentView
                model: model
             when "instrument" then view = new InstrumentView
                model: model
             else alert('Error: type ' + type + ' not recognized')
           @views[id] = view
           @changeView @views[id]
        else
           @changeView @views[id]

      # Direct and Router-based Editing Views
      createEditView: (model) ->
        switch model.get('type')
           when "experiment" then view = new ExperimentEdit
              model: model
           when "instrument" then view = new InstrumentEdit
              model: model
           when "treatment" then view = new TreatmentEdit
              model: model
           else alert('Error: type ' + model.get('type') + ' not recognized')
        view

      createObject: (type, id) =>
        @editObject type, id

      editObject: (type, id) =>
        if _.isObject type
           model = type
        else if type? and id?
           model = Backbone.ReferenceCache.resolve type, id
        else if type?
           model = new (Backbone.ReferenceCache.lookupConstructor(type))
           model.set('type', type)
        else
           alert 'Error, cannot edit unless type is provided'
        view = @editors[type]
        if not view?
           view = @createEditView(model)
           @editors[type] = view
           @changeView view
        else
           view.model = model
           @changeView view

      startTrial: (id) =>
        alert('not implemented')

      doSearch: (query, page) =>
        @results.doQuery query or "", page or 1
        @changeView @search
        @

      doTagSearch: (tag, page) =>
        @results.doTagQuery tag or "", page or 1
        @changeView @search
        @


    $(document).ready ->
       # Create the main app body
       window.Explore = new ExploreHome
          el: $('#explore')

       crumbs = new Breadcrumbs
          el: $('#crumbs')
          router: window.Explore.router
       crumbs.render()

       # Initialize navigation, resolve URL
       Backbone.history.start
          root: '/explore/'
          pushState: true

       window.Explore.render()
