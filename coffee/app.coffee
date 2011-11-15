#
# Routes, Application and Initialization for
#     InventHealth.com
#

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

# Resolving references utility
findModel = (models, id) ->
        models.find (mod) => mod.id == id

resolveReference = (ref) ->
        if ref
          alert "Bad Reference [#{ ref }]" if not _.isArray ref if ref
          switch ref[0]
            when 'experiment' then findModel window.Experiments, ref[1]
            when 'treatment' then findModel window.Treatments, ref[1]
            when 'instrument' then findModel window.Instruments, ref[1]
            when 'trial' then findModel window.MyTrials, ref[1]
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


class ModelCollection extends Backbone.Collection
  resolveReferences: =>
        @map (mod) -> mod.resolveRefs()

# MODEL INDEX

makeModelMap = ->
   map = {}
   for inst in window.Instruments.models
       map[inst.get 'id'] = inst
   for exp in window.Experiments.models
       map[exp.get 'id'] = exp
   for treat in window.Treatments.models
       map[treat.get 'id'] = treat
   for trial in window.MyTrials.models
       map[trial.get 'id'] = trial
   window.modelMap = map

window.lookupModels = (refs) ->
   models = []
   if _.isArray(refs) and _.isArray(refs[0])
        for [type, id] in refs
           model = window.modelMap[id]
           if model
                models.push model
           else
                alert "model not found #{ id }"
   else if _.isArray(refs) and _.isArray.length == 2
      model = window.modelMap[refs[1]]
      if model
          models.push window.modelMap[refs[1]]
      else
          alert "model not found #{ id }"
   models

#######################################################
# CONCRETE MODELS

# Suggestions
class Suggestion extends Backbone.Model

class Suggestions extends Backbone.Collection
  model: Suggestion

window.Suggestions = new Suggestions

# Treatments
class Treatment extends Model
  defaults:
        'tags': []
        'comments': []

  url: "/api/bone/treatment"

class Treatments extends ModelCollection
  model: Treatment
window.Treatments = new Treatments

# Instruments
class Instrument extends Model

class Instruments extends ModelCollection
  model: Instrument
window.Instruments = new Instruments

# Experiments
class Experiment extends Model
  dbrefs: [ 'instruments' ]

class Experiments extends ModelCollection
  model: Experiment
window.Experiments = new Experiments

# My Trials
class Trial extends Model
  dbrefs: [ 'experiment' ]

class Trials extends ModelCollection
  model: Trial
window.MyTrials = new Trials

# Comment model
class Comment extends Model
  defaults:
        'type': 'comment'

  initialize: (params, parent) ->
        @parent = parent
        alert 'No type specified' unless params.type
        if @get('responses')
           @responses = new CommentCollection @get('responses')

  voteUp: ->
        votes = @get('votes')
        unless votes[username]
                @set 'votes': votes[username] = 1

  voteDown: ->
        votes = @get('votes')[username]
        unless votes[username]
                @set 'votes': votes[username] = -1

  addComment: (params) ->
        response = new Comment params.extend 'parent'
        response.save()
        @set 'responses', @get('responses').push response

# User Object
# - Always initialized by the server
# - Singleton model, so use uppercase instance convention
class UserModel extends Backbone.Model
  username: -> @get('username')
  adminp: -> 'admin' in @get('permissions')
window.User = new UserModel


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

initCalendars = () ->
        $('.date_has_event').each calendarBehavior
        $('.date_has_event').each ->

## ------------------------
## Charts
## ------------------------

renderTrackerChart = (id, instrument, start, extra, options) ->
        options = $(options).extend
                chart:
                        type: 'spline'
                        renderTo: id

        $.getJSON "/api/charts/tracker",
                inst: instrument.get 'id'
                start: start,
                (config) ->
                        config = $.extend config, options
                        window.chart_config = config
                        new Highcharts.Chart config




####################################################
# Widgets and Base Classes                         #
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
        $(@el).html template model.toJSON()
        @

  inlineTemplate: (model, template) ->
        model = @resolveModel model
        template or= @template
        $(@el).append template model.toJSON()


#
# SwitchPane - A view that can be managed by a container, such as the switcher
#
class SwitchPane
  visiblep: =>
        if $(@el).is(':visible')
           true
        else
           false

  hidePane: =>
        if @visiblep()
           $(@el).hide()
#        promise = $.Deferred (dfd) => @el.fadeOut('fast', dfd.resolve)
#        promise.promise()

  showPane: =>
        if not @visiblep()
           $(@el).show()
#        promise = $.Deferred (dfd) => @el.fadeIn('fast', dfd.resolve)
#        promise.promise()

  dispatch: (path) =>
        @render()


#
# PaneSwitcher - Manages a set of AppView objects, only one of which
#    should be displayed at a time
#
class PaneSwitcher
  # List of panes
  panes: {}
  constructor: (panes) ->
        if typeof panes == 'undefined'
           return null
        if typeof panes == 'object'
           @panes = panes
        else
           alert 'Unrecognized format for panes ' + panes

  # Modify list
  add: (name, pane) ->
        @panes[name] = pane

  remove: (ref) ->
        if typeof ref == 'string'
           delete @panes[ref]
        else
           name = _.detect @panes, (name, pane) ->
                if (pane == ref)
                        name
           delete @panes[name]

  get: (name) ->
        pane = @panes[name]
        if typeof pane == 'undefined'
           alert 'Pane not found for name ' + name
        pane


  # Switch panes
  hideOtherPanes: (target) ->
        console.log @panes
        if not target
           (pane.hidePane() if pane) for name, pane of @panes
        else if typeof target == 'string'
           (pane.hidePane() if pane and name != target) for own name, pane of @panes
        else
           (pane.hidePane() if pane and pane != target) for own name, pane of @panes

  switch: (name) =>
        pane = @get name
        if pane == null
          alert 'no pane for name ' + name
        @hideOtherPanes name
        pane.showPane()


####################################################
# APPLICATION PANES
####################################################

#+++++++++++++++++++++
# Dashboard Page
#+++++++++++++++++++++

class DashboardApp extends Backbone.View
  @implements TemplateView, SwitchPane
  model: window.User
  initialize: ->
        @model.bind('change', @render) if @model
        @headerTemplate = @getTemplate '#dashboard-header'
        @trialsTemplate = @getTemplate '#trial-table'
#        @schedule = new ScheduleView
#        @journal = new JournalView
#        @discussions = new RecentDiscussionsView
#        @background = new BackgroundDataView

  events: {}

  dispatch: ->
        if $(@el).empty()
                @render()
  render: =>
        $(@el).append @headerTemplate @model.toJSON()
        $(@el).append @trialsTemplate {trials: window.MyTrials.toJSON()}
        @

class AdminApp extends Backbone.View
  @implements TemplateView, SwitchPane
  model: window.User
  initialize: ->
        @model.bind('change', @render) if @model
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
        @model = window.searchFilter or= new SearchFilterModel
        @initTemplate '#search-filter'
        @dialog = @getTemplate '#basic-dialog'

  update: (elt) =>
        results = $('input.as-values').attr('value').split(',').filter( (x) ->
                x.length > 1 )
        @model.set filters: results

  removeUpdate: (elt) =>
        elt.remove()
        @update()

  allObjects: ->
        window.Suggestions.toJSON()

  render: =>
        $(@el).empty()
        @inlineTemplate()
        message =
                title: "Search Help"
                body: "Type 'show X' to filter by experiment"
        $(@el).append @dialog message
        @

  finalize: =>
        defaults = searchSuggestDefaults
        defaults.selectionAdded = @update
        defaults.selectionRemoved = @removeUpdate
        $(@el).find('#search-filter-input').autoSuggest @allObjects(), defaults
        $('input.as-values').attr('value', ',')

  # Help Dialog
  events:
        'click a.help-link': 'showHelpDialog'

  openDialog: (d) =>
        @container = d.container[0]
        d.overlay.show()
        d.container.show()
        $('#osx-modal-content', @container).show()
        title = $('#osx-modal-title', @container)
        title.show()
        h = $('#osx-modal-data', @container).height() + title.height() + 30
        $('#osx-container').height(h)
        $('div.close', @container).show()
        $('#osx-modal-data', @container).show()


  showHelpDialog: (e) ->
        e.preventDefault()
        $('#osx-modal-content').modal
                overlayId: 'osx-overlay'
                containerId: 'osx-container'
                position: [100]
                closeHTML: null
                minHeight: 80
                opacity: 40
                overlayClose: true
                onOpen: @openDialog


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
        $(@el).html @template @model.toJSON()
        @

  inspect: ->
        window.socialView.setContext @model

  viewModel: =>
        window.socialView.setContext @model
        type = @model.get 'type'
        id = @model.get 'id'
        window.appRouter.navigate "/app/search/#{ type }/#{ id }", true

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
        window.listView = @ # NOTE: debug
        @views = []
        @templates = @buildTemplates()
        # Handle search bar changes
        @model.bind('change', @updateModels)
        # Handle new models
        @models = new Backbone.Collection
        @models.bind('reset', @updateView)
        @updateModels() # called when new filters added
        @

  # Handling changes
  allModels: ->
         window.Experiments.models.concat window.Treatments.models.concat window.Instruments.models

  selectModels: (selects) ->
        selects or= []
        if selects.length == 0
                models = @allModels()
                @models.reset models
        else
                $.get "/api/fsearch?query=#{ selects }&limit=#{ @limit }",
                      {},
                      ( refs, status, jqxhr ) =>
                          @models.reset window.lookupModels refs

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
        $(@el).empty()
        $(@el).append view.render().el for view in @views
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
        $(@el).show()
  hide: ->
        $(@el).hide()

  render: =>
        $(@el).append @filterView.el
        $(@el).append @listView.el
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
        $(@el).show()
  hide: ->
        $(@el).hide()

  setModel: (model) =>
        @model = model
        @render()

  # Rendering
  render: =>
        $(@el).empty()
        $(@el).append "<span class='breadcrumb'><a href='/app/search'>Search</a> -> #{ @model.attributes.type } -> <a href='/app/search/#{ @model.attributes.type }/#{ @model.attributes.id }'>#{ @model.attributes.name }</a></span>" if @model
        $(@el).append "<span class='breadcrumb'><a  href='/app/search'>Search</a></span>" unless @model
        $(@el).append @templateMap[@model.get 'type'] @model.toJSON() if @model
        @

  finalize: =>


# ----------------------------------------
#   Main browser window - rarely refreshed
# ----------------------------------------
class SearchApp extends Backbone.View
  @implements TemplateView, SwitchPane
  initialize: ->
        @search = new SearchView
        @search.render()
        @view = new ObjectView
        @

  # Dispatch object
  dispatchObject: (ref) =>
           models = lookupModels [ ref ]
           if models.length > 0
                @view.setModel models[0]
                document.title = models[0].get 'title'
                return true
           else
                alert "no model found for #{ ref }" unless models
                window.appRouter.navigate "/app/search", true
                return false

  # Dispatch view type
  dispatch: (path) =>
        if $(@el).children().size() == 0
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
        $(@el).empty()
        $(@el).append @search.render().el
        $(@el).append @view.render().el
        @search.finalize()
        @view.finalize()
        @

# ---------------------------------
#   Trial Viewer
# ---------------------------------
class TrialView extends Backbone.View
  @implements TemplateView
  initialize: (exp) ->
        @initTemplate '#trial-list-view'

  render: =>
        @renderTemplate()

  events:
        'click': 'viewModel'

  finalize: ->
        @delegateEvents()

  viewModel: () =>
        window.socialView.setContext @model.get 'experiment'
        window.socialView.enableEdit true
        id = @model.get('id')
        window.appRouter.navigate "/app/trials/#{ id }", true

class TrialApp extends Backbone.View
  @implements TemplateView, SwitchPane
  newTrial: (trial) ->
        new TrialView
                model: trial

  initialize: (exp) ->
        @views = window.MyTrials.map @newTrial
        @initTemplate '#trial-header'
        @chartTemplate = @getTemplate '#highchart-div'
        @journalTemplate = @getTemplate '#journal-entry'
        @

  dispatch: (path) ->
        if path
           model = findModel window.MyTrials, path
           alert "no model found for #{ path }" unless model
           @model = model
        else
           @model = null
        @render()

  render: =>
        if @model
           @renderModel()
        else
           @renderList()
        @

  renderModel: =>
        $(@el).empty()
        @inlineTemplate()
        @renderOutcome()
        @renderJournal()
        initCalendars()
        @

  renderOutcome: =>
        # TODO
        # Get instrument reference from trial
        # Make sure template renders correctly
        # Ensure that javascript renders in place
        experiment = @model.get 'experiment'
        outcome = experiment.get('instruments')[0]
        @inlineTemplate
                cssid: 'chart1'
                height: '150px'
                width: '500px',
                @chartTemplate
        renderTrackerChart 'chart1', outcome, 0

  renderJournal: =>
        @inlineTemplate entry, @journalTemplate for entry in @model.get 'journal'

  renderList: =>
        $(@el).empty()
        $(@el).append '<h1>My Trials</h1>'
        $(@el).append view.render().el for view in @views
        view.finalize() for view in @views
        @


# +++++++++++++++++++++
# Social Viewer
# +++++++++++++++++++++

# View dispatch sends message that new social data is available
# handler on social viewer grabs data from event and updates view
# to point at the current 'sociable' model

class SocialView extends Backbone.View
  @implements TemplateView
  initialize: (id) ->
        @el = $('#social' ? id)[0]
        @initTemplate '#comment-short-view'
        @edit = false
        @render()

  setContext: (model) ->
        @model = model
        @render()

  enableEdit: (flag) ->
        @edit = flag
        @render()

  render: =>
        $(@el).empty()
        $(@el).append '<h1>Discussion</h1>'
        if @edit
                $(@el).append "<a class='sview-add-link' href='#'>Add Comment</a><br/>"
        if @model
                comments = @model.get 'comments'
                @inlineTemplate c for c in comments if comments
        @

#                string = '<h2>'.concat(@model.get('type'), '</h2>')
#                $(@el).append string

##################################################################
# APPLICATION
#   Create main app panes, handle url dispatch, and frame elements
##################################################################

#
# AppRouter dispatches the main URL, creates and/or activates the
#
class AppRouter extends Backbone.Router
  initialize: ->
        @switcher = new PaneSwitcher
                'dashboard': new DashboardApp
                        el: $('#dashboardApp').first()
                'trials': new TrialApp
                        el: $('#trialApp').first()
                'search': new SearchApp
                        el: $('#searchApp').first()
                'admin': new AdminApp
                        el: $('#adminApp').first()

  # Routing
  routes:
        '/app/:app/*path': 'activate'
        '/app/:app/':      'activate'
        '/app/:app':       'activate'

  # Activate an app
  activate: (pname, path) =>
        console.log 'Activating ' + pname + ' with ' + path
        if typeof path == 'undefined'
            window.mainMenu.setMenu pname
        else
            window.mainMenu.setMenu pname + "/" + path
        pane = @switcher.get(pname)
        if pane
           @switcher.hideOtherPanes()
           pane.showPane()
           pane.dispatch path
        else
           @switcher.hideOtherPanes()
           $('#errorApp').show()
        @

#
# Main Menu Viewer
#
class MainMenu extends Backbone.View
   root: 'app'

   initialize: ->
        @root = @options.root ? @root
        @el = $(@options.elid).first()
        @delegateEvents()
        @

   setCurrent: (link) ->
        window.testlink2 = window.testlink
        window.testlink = link
        if link
                @$('a.current').removeClass('current')
                $(link).addClass 'current'

   setMenu: (base, path) ->
        window.testlast = 'setmenu'
        if path
           link = $("a[href='/#{@root}/#{base}/#{path}']").first()
           link.parents('ul.submenu').show()
           console.log(link)
        else
           link = $("a[href='/#{@root}/#{base}']").first()
           console.log(link)
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
        window.testlast = 'activate'
        @setCurrent newLink
        target = $(newLink).attr 'href'
        window.appRouter.navigate target, true
        @

# After the page is loaded, we have the skeleton in place
# and just need to dispatch to the app we need based on the
# rest of the URL.  Any other top-level initialization can be
# done here too

loadModels = ->
   if not window.User
      window.User.fetch()

$(document).ready ->
   loadModels()
   # Fix up internal references
   window.Instruments.resolveReferences()
   window.Experiments.resolveReferences()
   window.MyTrials.resolveReferences()
   window.Treatments.resolveReferences()
   makeModelMap()

   # Setup top level views
   window.socialView = new SocialView '#social'
   window.appRouter = new AppRouter
   window.mainMenu = new MainMenu {elid: '#main-menu'}

   # Initialize navigation, resolve URL
   match = Backbone.history.start
        pushState: true
        root: ''
   window.appRouter.navigate document.location.pathname, true
