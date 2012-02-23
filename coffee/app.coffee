#
# Routes, Application and Initialization for
#     PersonalExperiments.org
#

root = exports ? this

#
# CoffeeScript Extensions
#
implements = (classes...) ->
    for klass in classes
        # static properties
        for prop of klass
            @[prop] = klass[prop]
        # prototype properties
        for prop of klass.prototype
            getter = klass::__lookupGetter__(prop)
            setter = klass::__lookupSetter__(prop)

            if getter || setter
                @::__defineGetter__(prop, getter) if getter
                @::__defineSetter__(prop, setter) if setter
            else
                @::[prop] = klass::[prop]
    return this

if Object.defineProperty
    Object.defineProperty Function.prototype, "implements", value : implements
else
    Function::implements = implements

####################################################
# MODELS
####################################################

# Global Models (System State)
# - Load everything for now
# - To scale, lazy-load base collections as needed (TBD)

# NOTE: Roll this into the App object

# Resolving references utility
findModel = (models, id) ->
        models.find (mod) => mod.id == id

returnUser = (id) ->
        if id == App.User.get('id')
           return App.User
        else
           user = findModel App.Users, id
           if not user
              alert "Found object reference to another user #{ id }"
           else
              return user

resolveReference = (ref) ->
        if ref
          alert "Bad Reference [#{ ref }]" if not _.isArray ref if ref
          switch ref[0]
            when 'experiment' then findModel App.Experiments, ref[1]
            when 'treatment' then findModel App.Treatments, ref[1]
            when 'instrument' then findModel App.Instruments, ref[1]
            when 'trial' then findModel App.MyTrials, ref[1]
            when 'tracker' then findModel App.MyTrackers, ref[1]
            when 'user' then returnUser ref[1]
            else alert "Reference not found [#{ ref }]"

class Model extends Backbone.Model
  # Declarations for subclasses
  dbrefs: []
  resolveRefs: =>
        updates = {}
        for field in @dbrefs
                value = @get field
                if _.isArray _.first(value)
                    updates[field] = _.map(@get(field), resolveReference)
                else
                    updates[field] = resolveReference(@get(field))
        @set updates

  url: () ->
        type = @get 'type'
        id = @get 'id'
        "/api/bone/#{ type }/#{ id }"

  # Recursive toJSON to resolve models
  toJSON: () =>
        attrs = Backbone.Model.prototype.toJSON.call(this)
        for field in @dbrefs
                model = attrs[field]
                if _.isArray model
                   iter = (model) -> model.toJSON() if model
                   attrs[field] = _.map(model, iter) if model
                else
                   attrs[field] = attrs[field].toJSON()
        attrs

  # Modify state
  annotate: (type, text) =>
        id = @get 'id'
        mtype = @get 'type'
        $.post "/api/annotate/#{mtype}/#{id}/#{type}",
                text: text
        @fetch()

class ModelCollection extends Backbone.Collection
  resolveReferences: =>
        @map (mod) -> mod.resolveRefs()


#######################################################
# CONCRETE MODELS

# Suggestions
class Suggestion extends Backbone.Model

class Suggestions extends Backbone.Collection
  model: Suggestion


# Treatments
class Treatment extends Model
  defaults:
        'tags': []
        'comments': []

class Treatments extends ModelCollection
  model: Treatment

# Instruments
class Instrument extends Model

class Instruments extends ModelCollection
  model: Instrument

# Experiments
class Experiment extends Model
  dbrefs: [ 'instruments', 'treatment' ]

class Experiments extends ModelCollection
  model: Experiment

# My Trials
class Trial extends Model
  dbrefs: [ 'experiment' ]

class Trials extends ModelCollection
  model: Trial

# Trackers
class Tracker extends Model
  dbrefs: ['user', 'instrument']

class Trackers extends ModelCollection
  model: Tracker

# User Object
# - Always initialized by the server
# - Singleton model, so use uppercase instance convention
class UserModel extends Model
  username: -> @get('username')
  adminp: -> 'admin' in @get('permissions')

class UserModels extends ModelCollection
  model: UserModel

#########################################################
# Main Application Object

class Application extends Backbone.View
  el: $('#app-wrap')
  Instruments: new Instruments
  Treatments: new Treatments
  Experiments: new Experiments
  Suggestions: new Suggestions
  MyTrials: new Trials
  MyTrackers: new Trackers
  User: new UserModel
  Users: new UserModels

  start: () ->
    # Get the main user
    if not @User
       @User.fetch()

    # Resolve bootstrapped references
    @Instruments.resolveReferences()
    @Experiments.resolveReferences()
    @MyTrials.resolveReferences()
    @MyTrackers.resolveReferences()
    @Treatments.resolveReferences()
    @makeModelMap()

    # Populate and render main switcher panes
    @switcher = new PaneSwitcher
       id: 'app-tabs'
       panes:
           dashboard: new DashboardPane
           search: new SearchPane
           profile: new ProfilePane
           admin: new AdminPane
           trials: new TrialPane

    @router = new AppRouter
    @router.on 'route:selectApp', @switchApp

    @render()
    @

  render: =>
    @$el.append @switcher.render().el
    @


  # MODEL INDEX
  makeModelMap: ->
    map = {}
    classes = [@Instruments, @Experiments, @Treatments, @MyTrials, @MyTrackers]
    _(classes).each (c) -> for m in c.models
                                 map[m.get 'id'] = m
    @modelMap = map

  switchApp: (pname, path) =>
    if typeof path == 'undefined'
        @menu.setMenu pname
    else
        @menu.setMenu pname + "/" + path
    pane = @switcher.getPane(pname)
    if pane
        @switcher.hideOtherPanes()
        pane.showPane()
        pane.dispatch path
    else
        @switcher.hideOtherPanes()
        $('#errorApp').show()

  lookupModels: (refs) =>
    models = []
    if _.isArray(refs) and _.isArray(refs[0])
         for [type, id] in refs
            model = App.modelMap[id]
            if model
                 models.push model
            else
                 alert "model not found #{ id }"
    else if _.isArray(refs) and _.isArray.length == 2
       model = @modelMap[refs[1]]
       if model
           models.push @modelMap[refs[1]]
       else
           alert "model not found #{ id }"
    models

# Application instance and local/global aliases
window.ExApp = new Application
App = window.ExApp

###################################################
# Local Models
###################################################

# Client UI State Models
class BrowserFilter extends Backbone.Model
   defaults:
        'type': 'all'
        'sort': ['title', 'asc']

class BrowserModel extends Backbone.Model
   defaults:
        'state': 'list'

####################################################
# Utility Views
####################################################

## Events and Calendar

calendarBehavior = () ->
  distance = 10
  time = 250
  hideDelay = 500
  hideDelayTimer = null
  beingShown = false
  shown = false
  trigger = $(this)
  popup = $('.events ul', this).css('opacity', 0)

  $([trigger.get(0), popup.get(0)]).mouseover () ->
        if hideDelayTimer
                clearTimeout(hideDelayTimer)
        if beingShown or shown
                return
        else
                beingShown = true
        popup.css
                bottom: 20
                left: -76
                display: 'block'
        .animate {bottom: "+=#{ distance }px", opacity: 1}, time, 'swing', ->
                  beingShown = false
                  shown = true
   .mouseout () ->
           clearTimeout hideDelayTimer if hideDelayTimer
           popup.animate {bottom: "-=#{ distance }px", opacity: 0},time,'swing', ->
                   shown = false
                   popup.css 'display', 'none'

initCalendar = (url, id, month) ->
        $.get(url + id, {month: month}, (cal) =>
                $(id).html cal
                $('.date_has_event').each calendarBehavior)

## ------------------------
## Charts
## ------------------------

renderTrackerChart = (id, instrument, start, extra, options) ->
        options = $(options).extend
                chart:
                        type: 'line'
                        renderTo: id

        $.getJSON "/api/charts/tracker",
                inst: instrument.get 'id', (config) ->
                        config = $.extend config, options
                        App.chart_config = config
                        new Highcharts.Chart config


####################################################
# View Widgets and Base Classes                    #
####################################################

#
# TemplateView - default template support for Backbone.View apps
#
class TemplateView
  getTemplate: (name) ->
        try
          html = $(name).html()
          alert 'No template found for #{ name }' unless name
          Handlebars.compile html
        catch error
          alert "Error loading template #{ name } ... #{ error }"

  initTemplate: (name) ->
        @template = @getTemplate name
        @

  resolveModel: (model) ->
        if not model
           model = @model
        if not model
           model = new Backbone.Model({})
        if not model.toJSON
           model = new Backbone.Model(model)
        model

  renderTemplate: (model, template) ->
        model = @resolveModel model
        template or= @template
        @$el.html template model.toJSON()
        @

  inlineTemplate: (model, template) ->
        model = @resolveModel model
        template or= @template
        @$el.append template model.toJSON()


#
# SwitchPane - A view that can be managed by a container, such as the switcher
#
class SwitchPane
  visiblep: =>
        if @$el.is(':visible')
           true
        else
           false

  hidePane: =>
        if @visiblep()
#           @$el.fadeOut('fast')
           @$el.hide()
#        promise = $.Deferred (dfd) => @el.fadeOut('fast', dfd.resolve)
#        promise.promise()

  showPane: =>
        if not @visiblep()
#           @$el.fadeIn('fast')
           @$el.show()
#        promise = $.Deferred (dfd) => @el.fadeIn('fast', dfd.resolve)
#        promise.promise()

  dispatch: (path) =>
         @


#
# PaneSwitcher - Manages a set of AppView objects, only one of which
#    should be displayed at a time
#
class PaneSwitcher extends Backbone.View
  # List of panes
  panes: {}
  initialize: ->
        @panes = @options.panes
        @

  render: =>
        @$el.empty()
        @$el.append pane.render().el for name,pane of @panes
        @

  # Switch panes
  hideOtherPanes: (target) ->
        if not target
           (pane.hidePane() if pane) for name, pane of @panes
        else if typeof target == 'string'
           (pane.hidePane() if pane and name != target) for own name, pane of @panes
        else
           (pane.hidePane() if pane and pane != target) for own name, pane of @panes

  switch: (name) =>
        @active = name
        pane = @getPane name
        if pane == null
          alert 'no pane for name ' + name
        @hideOtherPanes name
        pane.showPane()

  # Modify list
  addPane: (name, pane) ->
        @panes[name] = pane
        @trigger('switcher:add')
        @render()
        @

  removePane: (ref) ->
        if typeof ref == 'string'
           delete @panes[ref]
        else
           name = _.detect @panes, (name, pane) ->
                if (pane == ref)
                        name
           delete @panes[name]
        @trigger('switcher:remove')
        @render()
        @

  getPane: (name) ->
        pane = @panes[name]
        if typeof pane == 'undefined'
           alert 'Pane not found for name ' + name
        pane


#
# JOURNAL VIEW
#
class JournalView extends Backbone.View
  @implements TemplateView
  initialize: ->
        @initTemplate '#journal-viewer'
        @model.on('change', @render) if @model
        @mtype = @options.type
        @paging = @options.paging
        @page = @options.page or 1
        @size = @options.pagesize or 1
        @editable = @options.editable or false
        @editing = false

  render: =>
        if not @model
                @$el.html("<h3>No Journal Entries Found</h3>")
                return @
        else
                entries = @model.get('journal')
                entries = _.sortBy(entries, (x) -> x.date).reverse() if entries
                if @options.paging
                        base = (@page - 1) * @size
                        bounds = @page * @size
                        entries = _.toArray(entries).slice(base, bounds)
                @$el.empty()

                args =
                        page: @page
                        total: Math.ceil @model.get('journal').length / @size
                        entries: entries
                if @mtype
                        args['type'] = @mtype
                        args['context'] = " " #@model.get('experiment').get('title')
                @inlineTemplate args

        if @editing
                @editView()
        else
                @journalView()
        @$('button.prev').attr('disabled', true) if @page == 1
        @$('button.next').attr('disabled', true) if (@page * @size) >= @model.get('journal').length
        @

  finalize: ->
        @journalView()
        @delegateEvents()

  events:
        'click .create': 'edit'
        'click .submit': 'submit'
        'click .cancel': 'cancel'
        'click .next': 'nextPage'
        'click .prev': 'prevPage'

  journalView: ->
        @$('div.edit').hide()
        @$('div.create').show() if @editable
        @$('div.journal-entry').show()

  editView: ->
        @$('div.create').hide()
        @$('div.journal-entry').hide()
        @$('div.edit').show()

  nextPage: =>
        unless @editing or (@page * @size) > @model.get('journal').length - 1
                @page = @page + 1
                @render()

  prevPage: =>
        unless @editing or @page <= 1
                @page = @page - 1
                @render()

  edit: =>
        @editing = true
        @editView()
        @

  submit: =>
        @model.annotate 'journal', @$('textarea').val()
        @journalView()
        @page = 1
        @editing = false
        @

  cancel: =>
        @journalView()
        @editing = false


####################################################
# APPLICATION PANES
####################################################

#+++++++++++++++++++++
# Dashboard Page
#+++++++++++++++++++++

class SummaryView extends Backbone.View
  @implements SwitchPane, TemplateView
  id: 'dashboard-summary'
  className: 'dashboard-summary'
  initialize: ->
        @trialsTemplate = @getTemplate '#trial-table'
        @views = App.MyTrials.map @newTrial
        App.MyTrials.on('change', @render)
        @render()

  newTrial: (trial) =>
        new TrialSummaryView
                model: trial

  render: =>
        @$el.empty()
        @$el.append '<h1>My Trials</h1>'
        if _.isEmpty @views
                @$el.append "<h3>No Active Trials</h3>"
        else
                @$el.append view.render().el for view in @views
                view.finalize() for view in @views
        @$el.append '<div class="reminders"><h2>Reminders</h2><p>&nbsp;&nbsp;[Reminders are a list of upcoming measurement/treatment events]</p></div>'
        @$el.append '<div class="feeds"><h2>Feeds</h2><p>&nbsp;&nbsp;[Feeds are a list of comments / activity around experiments you are involved in or "watching"]</p></div>'
        @

  finalize: ->

#        @feedTemplate = @getTemplate '#feed-list'
#        @schedule = new ScheduleView
#        @discussions = new RecentDiscussionsView

class TrackerView extends Backbone.View
  @implements SwitchPane, TemplateView
  className: 'dashboard-tracker'

  initialize: ->
        @initTemplate '#highchart-div'
        @trackers = App.MyTrackers.models
        @

  render: =>
        @$el.empty()
        @$el.append '<h1>Tracking Data</h1>'
        if _.isEmpty @trackers
                @$el.append '<h3>No Tracking Data Found</h3>'
        else
                @renderChart @getID tracker for tracker in @trackers
        @

  finalize: ->
        for tracker in @trackers
                cssid = @getID tracker
                inst = tracker.get('instrument')
                renderTrackerChart cssid, inst, 0

  getID: (tracker) ->
        if tracker
                tracker.get('id')
        else
                alert 'Unrecognized tracker'

  renderChart: (id) ->
        @inlineTemplate
                cssid: id
                height: '150px'
                width: '500px'


class JournalWrapper extends Backbone.View
  @implements SwitchPane
  className: 'dashboard-journal'
  initialize: ->
        @view = new JournalView
                className: 'dashboard-journal'
                model: App.MyTrials.models[0]
                paging: true
                pagesize: 5
                editable: false
        @

  render: =>
        @$el.empty()
        @$el.append @view.render().el if @view
        @

  finalize: ->

class DashboardPane extends Backbone.View
  @implements TemplateView, SwitchPane
  id: 'dashboard'
  model: App.User
  initialize: ->
        @initTemplate '#dashboard-header'
        @switcher = new PaneSwitcher
                id: 'dashboard-switcher'
                panes:
                   overview: new SummaryView
                   journal: new JournalWrapper
                   tracking: new TrackerView
        @switcher.switch('overview')
        @

  dispatch: (path) ->
        if @$el.children().size() == 0
           @render()
        if path
           ref = path.split("/") if path
           @$('a.tab').removeClass('active-tab')
           @$("a[href='dashboard/#{path}']").addClass('active-tab')
           @switcher.switch(ref[0])
        else
           App.router.navigate "dashboard/#{@switcher.active}", true

  render: =>
        @renderTemplate()
        @$el.append @switcher.render().el
        pane.finalize() for name,pane of @switcher.panes
        @

  events:
        'click a.tab': 'switch'

  switch: (e) =>
        e.preventDefault()
        tabpath = $(e.target).attr('href')
        App.router.navigate tabpath, true

# ++++++++++++++++++++++++++++++
# Admin App
# ++++++++++++++++++++++++++++++

class AdminPane extends Backbone.View
  @implements TemplateView, SwitchPane
  id: 'admin'
  model: App.User
  initialize: ->
        @model.on('change', @render) if @model
        @initTemplate '#admin-main'

  render: =>
        @renderTemplate()

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

# ---------------------------------
#   Trial Viewer
# ---------------------------------
class TrialSummaryView extends Backbone.View
  @implements TemplateView
  initialize: (exp) ->
        @initTemplate '#trial-list-view'

  render: =>
        @renderTemplate()

  events:
        'click': 'viewModel'

  finalize: ->
        @delegateEvents()

  viewModel: =>
        App.socialView.setContext model.get 'experiment'
        App.socialView.setEdit true
        id = @model.get('id')
        App.router.navigate "trials/#{id}", true

class TrialPane extends Backbone.View
  @implements TemplateView, SwitchPane
  id: 'trial'
  newTrial: (trial) ->
        new TrialSummaryView
                model: trial

  initialize: (exp) ->
        @views = App.MyTrials.map @newTrial
        @initTemplate '#trial-view-header'
        @chartTemplate = @getTemplate '#highchart-div'
        @calendarTemplate = @getTemplate '#small-calendar'
        @instTableTemplate = @getTemplate '#instrument-short-table'
        @

  dispatch: (path) ->
        if path
           @model = findModel App.MyTrials, path
           alert "no model found for #{ path }" unless @model
           @journal = new JournalView
                className: 'trial-journal'
                type: 'Trial'
                model: @model
                paging: true
                editable: true
                page: 1
                pagesize: 1
           @viewModel @model
        else
           App.socialView.setEdit false
           @model = null
        @render()

  render: =>
        if @model
           @renderModel()
        else
           @renderList()
        @

  viewModel: (model) =>
        App.socialView.setContext model.get 'experiment'
        App.socialView.setEdit true

  renderModel: ->
        experiment = @model.get 'experiment'
        treatment = experiment.get 'treatment'
        outcome = experiment.get 'outcome'
        @$el.empty()
        @inlineTemplate()
        @renderCalendar(experiment)
        @renderInstruments(experiment)
        @$el.append "<div class='clear'/>"
        @renderOutcome(experiment) # TODO: outcome)
        @$el.append @journal.render().el
        @journal.finalize()
        @

  renderOutcome: (outcome) ->
        # TODO
        # Get instrument reference from trial
        # Make sure template renders correctly
        # Ensure that javascript renders in place
        outcome = outcome.get('instruments')[0]
        @$el.append '<h1>Results</h1>'
        @inlineTemplate
                cssid: 'chart1'
                height: '150px'
                width: '500px',
                @chartTemplate
        renderTrackerChart 'chart1', outcome, 0

  renderCalendar: (experiment) ->
        mydiv = $("<div class='trial-calendar-wrap'/>")
        @$el.append mydiv
        mydiv.append '<h2>Schedule</h2>'
        id = experiment.get 'id'
        month = "now"
        mydiv.append @calendarTemplate {id: 'trial-cal'}
        initCalendar("/api/calendar/experiment/#{ id }", '#trial-cal', month)

  renderInstruments: (experiment) ->
        mydiv = $("<div class='trial-measures'/>")
        @$el.append mydiv
        mydiv.append '<h2>Tracking Instruments</h2>'
        instruments = experiment.get('instruments')
        data = {instruments: _.map(instruments, (x) -> x.toJSON())}
        mydiv.append @instTableTemplate data

  renderTrials: (trialViews) ->
        mydiv = $("<div class='trial-views'/>")
        mydiv.append '<h1>My Trials</h1>'
        mydiv.append view.render().el for view in @views
        view.finalize() for view in @views
        @$el.append mydiv
        @

  renderList: =>
        @$el.empty()
        @renderTrials()
        @


# +++++++++++++++++++++
# Social Viewer
# +++++++++++++++++++++

# View dispatch sends message that new social data is available
# handler on social viewer grabs data from event and updates view
# to point at the current 'sociable' model

class SocialView extends Backbone.View
  @implements TemplateView
  initialize: () ->
        @initTemplate '#comment-short-view'
        @edit = false
        @render() if @model
        @

  setContext: (model) ->
        @model.off('change') if @model
        @model = model
        @model.on('change', @render)
        @edit = false
        @render()

  setEdit: (flag) ->
        @edit = flag
        @render()

  render: =>
        @$el.empty()
        @$el.append '<h1>Public Comments</h1>'
        if @edit
           @$el.append "<textarea rows='5' cols='20'/><button class='comment' type='button'>Comment</button>"
        if @parent
           comments = @parent.get 'comments'
           @inlineTemplate c for c in _.sortBy(comments, (x) -> x.date).reverse() if comments
        @delegateEvents()
        @

  # Events
  events:
        'click button.comment': 'addComment'

  addComment: =>
        @parent.annotate 'comment', @$('textarea').val()

# ++++++++++++++++++++++++++++++
# Profile App
# ++++++++++++++++++++++++++++++

class ProfilePane extends Backbone.View
  @implements TemplateView, SwitchPane
  id: 'profile'
  model: App.User
  initialize: ->
#        @model.on('change', @render) if @model
        @initTemplate '#profile-edit-view'

  events:
        'change': 'updateModel'

  updateModel: (e) =>
        e.preventDefault()
        target = $(e.target)
        name = target.prop 'name'
        @model.set name, target.val()
        @

  render: =>
        @renderTemplate @model.get 'prefs'

##################################################################
# APPLICATION
#   Create main app panes, handle url dispatch, and frame elements
##################################################################

#
# AppRouter dispatches the main URL, creates and/or activates the
#
class AppRouter extends Backbone.Router
  # Routing
  routes:
        ':app/*path': 'select'
        ':app':       'select'

  # Activate an app
  select: (pname, path) =>
        @trigger "route:selectApp", pname, path

#
# Main Menu View
#
class MainMenu extends Backbone.View
   urlBase: '/app/'
   initialize: ->
        @delegateEvents()
        @

   setCurrent: (link) ->
        if link
                @$('a.current').removeClass('current')
                $(link).addClass 'current'

   setMenu: (base, path) ->
        if path
           link = $("a[href='#{this.urlBase}/#{base}/#{path}']").first()
           link.parents('ul.submenu').show()
        else
           link = $("a[href='#{this.urlBase}/#{base}']").first()
        if link
           @setCurrent link

   events:
        'click a.expand': 'expand'
        'click a.action': 'activate'

   expand: (event) =>
        event.preventDefault()
        sublist = $(event.target).next()
        sublist.slideToggle 'fast'

   activate: (event) =>
        event.preventDefault()
        newLink = event.target
        @setCurrent newLink
        target = $(newLink).attr 'href'
        App.router.navigate target, true
        @

$(document).ready ->
   # Instantiate the SocialView
   App.socialView = new SocialView {el: $('#share-pane')}

   # Attach menu logic separately
   App.menu = new MainMenu {el: $('#main-menu')}

   # Create the main app body
   App.start()

   # Initialize navigation, resolve URL
   match = Backbone.history.start
        root: '/app/'
        pushState: true
        silent: false
