define ['models/infra', 'models/core', 'models/user', 'views/widgets', 'views/scheduling', 'views/common', 'use!Handlebars', 'use!BackboneFormsBS', 'use!BackboneFormsEditors'],
  (Infra, Core, User, Widgets, Scheduling, Common) ->

# Item Views
# --------------------------------------------

    class ItemView extends Backbone.View
      # Common Handlers
      addTags: (event) =>
        event.preventDefault()
        if not @tagDialog?
          @tagDialog = new Widgets.AddTagDialog()
          @tagDialog.on 'form:accept', (result) ->
             @model.addTagString result.tags
          , @
        @tagDialog.show()


#
# Experiments
#
    class ExperimentView extends ItemView
      initialize: (options) ->
        @template = Infra.templateLoader.getTemplate 'experiment-view'
        @model.on 'change', @render, @
        @

      render: ->
        if @model.isLoaded()
           @$el.html @template @model.toTemplateJSON()
        else if not @model._loading
           @model.fetch()
        @

      events:
        'click .add-tag': 'addTags'

#
# Treatments
#
# Experiments (membership), Instruments (via tags)
#
    class TreatmentView extends ItemView
      initialize: (options) ->
        @template = Infra.templateLoader.getTemplate 'treatment-view'
        @model.on 'change', @render, @
        @

      render: ->
        if @model.isLoaded()
           @$el.html @template @model.toTemplateJSON()
        else if not @model._loading
           @model.fetch()
        @

      events:
        'click .add-tag': 'addTags'


#
# Instruments
#
# Experiments (membership), Treatments (via tags)
#
    class InstrumentView extends ItemView
      initialize: (options) ->
        @template = Infra.templateLoader.getTemplate 'instrument-view'
        @model.on 'change', @render, @
        @

      render: ->
        if @model.isLoaded()
           @$el.html @template @model.toTemplateJSON()
        else if not @model._loading
           @model.fetch()
        @

      events:
        'click .track': 'track'
        'click .untrack': 'untrack'
        'click .edit': 'edit'
        'click .add-tag': 'addTags'

      track: (event) =>
        schedule = Scheduling.configureSchedule @model
        @model.track schedule if schedule?

      untrack: (event) =>
        @model.untrack()

      edit: (event) =>
        @trigger 'edit', @model




# Item Creators
# --------------------------------------------

    class ExperimentEdit extends Backbone.View
      initialize: (options) ->
        @template = Infra.templateLoader.getTemplate 'experiment-editor'
        @

      render: ->
        if @model.isLoaded()
           @$el.html @template @model.toTemplateJSON()
        else if not @model._loading?
           @model.fetch()
        @


    class TreatmentEdit extends Backbone.View
      initialize: (options) ->
        @template = Infra.templateLoader.getTemplate 'experiment-editor'
        @

      render: ->
        @$el.html @template @model.toTemplateJSON()
        @

    class ScheduleSchemaCreator extends Backbone.View

    class ScheduleInstantiator extends Backbone.View


# Search results and state
# --------------------------------------------

    # Search results model
    class SearchResults extends Backbone.Collection
      initialize: (options) ->
        @query = ""
        @page = 1
        if options? and options.pagesize?
           @size = options.pagesize
        else
           @size = 10
        @

      reset: (results, options) ->
        models = _.map results, (object) ->
           Backbone.ReferenceCache.import object
        super models, options
        @

      doQuery: (query, page) ->
        @query = query or @query
        @page = page or @page or 1
        offset = (@page - 1) * @size
        $.ajax "/api/search/#{@query}/#{@size}/#{offset}",
          context: @
          success: (results) =>
            @reset results

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
        @model.on 'change', @render, @
        @

      render: ->
        if @model? and @model.isLoaded()
           @$el.html @template @model.toTemplateJSON()
           @$el.append '<hr>'
        @

      events: ->
        'click a': 'viewObject'

      viewObject: (event) =>
        event.preventDefault()
        id = $(event.currentTarget).attr('data-id')
        object = Backbone.ReferenceCache.resolve null, id
        link = "/view/" + object.get('type') + "/" + id
        window.Explore.router.navigate link,
          trigger: true


    # Result page and handlers
    class SearchPage extends Backbone.View
      # STATE
      initialize: (options) ->
        # View maintenance and rendering
        @template = Infra.templateLoader.getTemplate 'search-header'
        @views = []

        # Result objects
        @results = options.results
        @results.on 'reset', @updateViews
        @render()
        @

      # MODEL EVENTS
      updateViews: () =>
        # Update query box
        @$('.search-query').val([@results.query])
        @renderViews()

      # RENDERING
      render: ->
        # Render or update search bar
        if @$('.search-query').length == 0
           @$el.append @template @query
        @

      renderViews: ->
        # Remove if exist
        _.each @views, (view) -> view.remove()
        # Create new
        @views = @results.map (model) ->
           new ResultView
             model: model
        # Render new
        _.each @views, (view) =>
           @$el.append view.render().el
        @

      # UI EVENTS
      events:
        'keyup': 'handleKey'
        'click .search-btn': 'submitQuery'

      handleKey: (event) =>
        if event.which is 13
           @submitQuery(event)
           false
        else
           true

      submitQuery: (event) =>
        event.preventDefault()
        query = @$('input').val() or ""
        window.Explore.router.navigate '/search/query/' + query,
          trigger: true
        true




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
          'edit/:type/': 'createObject'
          'start/:id': 'startTrial'

      selectDefault: (data) =>
          @navigate @default, {trigger: true, replace: true} if @default?

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
        @model = Backbone.ReferenceCache.resolve(type, id)
        @model.on 'change', @rerenderObj, @
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
             name: @model.name()
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
        @currentView = @search

        # Change views based on router events
        @router = new ExploreRouter
          default: 'search/query'
        @router.on 'route:viewObject', @viewObject
        @router.on 'route:editObject', @editObject
        @router.on 'route:startTrial', @startTrial
        @router.on 'route:searchQuery', @doSearch
        @router.on 'route:searchTag', @doSearch
        @

      # Common events
      events:
        'click .tags .label': 'searchTagClick'

      # Common UI event handlers
      searchTagClick: (event) =>
        event.preventDefault()
        tag = $(event.currentTarget).text()
        link = "/search/tag/" + tag
        window.Explore.router.navigate link,
          trigger: true

      # Route Event handlers
      viewObject: (type,id) =>
        if not @views[id]?
           model = Backbone.ReferenceCache.resolve type, id
           switch type
             when "experiment" then view = new ExperimentView
                model: model
             when "treatment" then view = new TreatmentView
                model: model
             when "instrument" then view = new InstrumentView
                model: model
             else alert('error, type ' + type + ' not recognized')
           @views[id] = view
        @changeView @views[id]

      editObject: (type,id) =>
        if _.isObject type
           model = type
        else
           model = Backbone.ReferenceCache.resolve type, id
        if not @editors[id]?
           switch type
             when "experiment" then view = new ExperimentEdit
                model: model
             when "treatment" then view = new TreatmentEdit
                model: model
             else alert('error, type ' + type + ' not recognized')
           @editors[id] = view
        @changeView @editors[id]

      startTrial: (id) =>
        alert('not implemented')

      doSearch: (query, page) =>
        @results.doQuery query or "p", page
        @changeView @search
        @

      # View utilities
      changeView: (view) ->
        @currentView.off 'edit', @editObject if @currentView
        @currentView = view
        @currentView.on 'edit', @editObject, @
        @render()

      render: ->
        @$el.children().detach()
        @$el.append @currentView.render().el if @currentView
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
























    # ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    # OLD MODELS AND VIEWS!!!
    # ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    # -----------------------------
    #   NLP Filter Box
    # -----------------------------

    # The BB Model for the current filter state
    class SearchFilterModel extends Backbone.Model

    # Configuration for the autocomplete
    searchSuggestDefaults =
       startText: 'What are you looking for?'
       keyDelay: 200
       resultsHighlight: false
       neverSubmit: true
       retrieveLimit: 20
       selectedValuesProp: 'value'
       selectedItemProp: 'title'
       searchObjProps: 'trigger,title,search'
       resultsHighlight: false

    # The UI View for the filter state, handles autocomplete for phrases
    class SearchFilterView extends Backbone.View
#      @implements TemplateView
      initialize: ->
            @model = App.searchFilter or= new SearchFilterModel
            @initTemplate '#search-filter'

      update: (elt) =>
            results = $('input.as-values').attr('value').split(',').filter( (x) ->
                    x.length > 1 )
            @model.set filters: results

      removeUpdate: (elt) =>
            elt.remove()
            @update()

      allObjects: ->
            App.Suggestions.toJSON()

      render: =>
            @inlineTemplate()
            @

      finalize: =>
            defaults = searchSuggestDefaults
            defaults.selectionAdded = @update
            defaults.selectionRemoved = @removeUpdate
            @$el.find('#search-filter-input').autoSuggest @allObjects(), defaults
            $('input.as-values').attr('value', ',')

    # Help Dialog
      events:
            'click a.help-link': 'showHelpDialog'

      showHelpDialog: (e) ->
            e.preventDefault()
            root.renderDialog "Search Help", "Type 'show X' to filter by experiment"


    # -----------------------------
    #   NLP filter list according to selected tags
    # -----------------------------

    # Views for individual models
    # - requires @model and @parent to be valid
    class SearchItemView extends Backbone.View
      className: 'search-list-item'
      initTemplate: ->
            @template = @parent.lookupTemplate @model

      initialize: ->
            @parent = @options.parent
            @initTemplate()
            @

      events:
            'mouseover': 'inspect'
            'click': 'viewModel'

      render: =>
            @$el.html @template @model.toJSON()
            @

      inspect: =>
            App.socialView.setContext @model

      viewModel: =>
            App.socialView.setContext @model
            type = @model.get 'type'
            id = @model.id
            App.router.navigate "search/#{ type }/#{ id }", true

    #
    # The UI View for the filtered object set
    #
    class SearchListView extends Backbone.View
      # State
      limit: 20

      # View configuration and setup
      className: 'search-list'

      subviews:
            experiment: '#experiment-list-view'
            treatment: '#treatment-list-view'
            instrument: '#instrument-list-view'

      buildTemplates: ->
            results = {}
            _.map @subviews, (id, type) ->
                    results[type] = Handlebars.compile $(id).html()
            results

      lookupTemplate: (model) ->
            type = model.get 'type'
            @templates[type]

      initialize: ->
            @views = []
            @templates = @buildTemplates()
            # Handle search bar changes
            @model.on('change', @updateModels)
            # Handle new models
            @models = new Backbone.Collection
            @models.on('reset', @updateView)
            @updateModels() # called when new filters added
            @

      # Handling changes
      allModels: ->
             App.Experiments.models.concat App.Treatments.models.concat App.Instruments.models

      selectModels: (selects) ->
            selects or= []
            if selects.length == 0
                    models = @allModels()
                    @models.reset models
            else
                    $.get "/api/fsearch?query=#{ selects }&limit=#{ @limit }",
                          {},
                          ( refs, status, jqxhr ) =>
                              @models.reset App.lookupModels refs

      updateModels: =>
            filters = @model.get 'filters'
            @selectModels filters



      # Rendering current result list
      asItemView: (model) =>
            new SearchItemView
                    model: model
                    parent: this

      updateView: =>
            @views = _.map(@models.models, @asItemView)
            @render()

      render: =>
            @$el.empty()
            @$el.append view.render().el for view in @views
            @

      finalize: ->
            view.delegateEvents() for view in @views

    class SearchView extends Backbone.View
#      @implements TemplateView
      className: "search-view"
      initialize: ->
            @filterView = new SearchFilterView
            @filterView.render()
            @listView = new SearchListView
                    model: @filterView.model
            @listView.render()

      show: ->
            @$el.show()
      hide: ->
            @$el.hide()

      render: =>
            @$el.append @filterView.el
            @$el.append @listView.el
            @

      finalize: =>
            @filterView.finalize()
            @listView.finalize()

    #
    # SINGLE OBJECT VIEW
    #
    class ObjectView extends Backbone.View
#      @implements TemplateView
      className: "object-view"
      viewMap:
            experiment: '#experiment-view'
            treatment: '#treatment-view'
            instrument: '#instrument-view'
      templateMap: {}
      initialize: ->
            try
                    _.map @viewMap, (id, type) =>
                            @templateMap[type] = Handlebars.compile $(id).html()
            catch error
                    alert 'Failed to load object view templates'

      # Actions
      show: ->
            @$el.show()

      hide: ->
            @$el.hide()

      setModel: (model) =>
            @model = model
            @render()

      # Rendering
      render: =>
            @$el.empty()
            @$el.append "<span class='breadcrumb'><a href='search'>Search</a> -> <a href='search'>#{ @model.attributes.type }</a> -> <a href='search/#{ @model.attributes.type }/#{ @model.attributes.id }'>#{ @model.attributes.name }</a></span>" if @model
            @$el.append "<span class='breadcrumb'><a  href='search'>Search</a></span>" unless @model
            @$el.append @templateMap[@model.get 'type'] @model.toJSON() if @model
            if @model
                App.socialView.setContext @model
                App.socialView.setEdit true
            @

      finalize: =>
            @delegateEvents()

      # Events
      events:
            'click .run': 'startExperiment'

      startExperiment: (e) =>
            e.preventDefault()
            root.renderDialog "Configure Experiment", "Placeholder Dialog pending Forms Package"

    # ----------------------------------------
    #  Main browser window - rarely refreshed
    # ----------------------------------------
    class SearchPane extends Backbone.View
#      @implements TemplateView, SwitchPane
      id: 'search'
      initialize: ->
            @search = new SearchView
            @search.render()
            @view = new ObjectView
            @

      # Dispatch object
      dispatchObject: (ref) =>
               models = App.lookupModels [ ref ]
               if models.length > 0
                    @view.setModel models[0]
                    document.name = models[0].get 'name'
                    return true
               else
                    alert "no model found for #{ ref }" unless models
                    App.router.navigate "search", true
                    return false

      # Dispatch view type
      dispatch: (path) =>
            if @$el.children().size() == 0
               @render()

            ref = path.split("/") if path
            if ref and ref.length = 2
               if @dispatchObject(ref)
                  @search.hide()
                  @view.show()
            else
               @search.show()
               @view.hide()
            @

      render: =>
            @$el.empty()
            @$el.append @search.render().el
            @$el.append @view.render().el
            @search.finalize()
            @view.finalize()
            @

    {}