    # ++++++++++++++++++++++++++++++
    # Search App
    # ++++++++++++++++++++++++++++++

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
      @implements TemplateView
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
            @$el.empty()
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
            id = @model.get 'id'
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
      @implements TemplateView
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
      @implements TemplateView
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
      @implements TemplateView, SwitchPane
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

