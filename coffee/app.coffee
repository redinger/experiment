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

# Experiments
class Experiment extends Backbone.Model

class Experiments extends Backbone.Collection
  model: Experiment
window.Experiments = new Experiments

# Treatments
class Treatment extends Backbone.Model
  defaults:
        'tags': []
        'comments': []

  tag: (tag) =>
        @set 'tags': @get('tags').push(tag)

  comment: (comment) =>
        @set 'comments': @get('comments').push(comment)

  comments: =>
        @get 'comments'

  url: "/api/bone/treatment"

class Treatments extends Backbone.Collection
  model: Treatment
window.Treatments = new Treatments

# Instruments
class Instrument extends Backbone.Model
  comments: =>
        @get 'comments'

class Instruments extends Backbone.Collection
  model: Instrument
window.Instruments = new Instruments

# Comment model
class Comment extends Backbone.Model
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

class Comments extends Backbone.Collection
  model: Comment

window.FocusComments = new Comments


# User Object
# - Always initialized by the server
# - Singleton model, so use uppercase instance convention
class UserModel extends Backbone.Model
  username: -> @get('username')
  adminp: -> 'admin' in @get('permissions')
window.User = new UserModel


# My Trials
class Trial extends Backbone.Model

class Trials extends Backbone.Collection
  model: Trial
window.MyTrials = new Trials

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
# Widgets and Base Classes                         #
####################################################

#
# TemplateView - default template support for Backbone.View apps
#
class TemplateView
  initTemplate: (name) ->
        @template = Handlebars.compile $(name).html()
        @

  resolveModel: (model) ->
        if not model
           model = @model
        if not model
           model = new Backbone.Model({})
        if not model.toJSON
           model = new Backbone.Model(model)
        model

  renderTemplate: (model) ->
        model = @resolveModel model
        $(@el).html @template model.toJSON()
        @

  inlineTemplate: (model) ->
        model = @resolveModel model
        @template model.toJSON()


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
        @initTemplate '#dashboard-main'

  events: {}

  render: =>
        @renderTemplate()

class AdminApp extends Backbone.View
  @implements TemplateView, SwitchPane
  model: window.User
  initialize: ->
        @model.bind('change', @render) if @model
        @initTemplate '#admin-main'

  render: =>
        @renderTemplate()

# ++++++++++++++++++++++++++++++
# Discover App
# ++++++++++++++++++++++++++++++

# -----------------------------
#   NLP Filter Box
# -----------------------------

# The BB Model for the current filter state
class DiscoverFilterModel extends Backbone.Model
  initialize: ->
        @phrases = []

  addPhrase: (text) =>
        @phrases.push text

  removePhrase: (text) =>
        @phrases = _.difference @phrases, [ text ]

# Configuration for the autocomplete
preFill = [ { value: "show experiments" } ]
discoverSuggestDefaults =
  startText: 'What are you searching for?'
  preFill: preFill
  keyDelay: 200
  resultsHighlight: false
  neverSubmit: true
  retrieveLimit: 20
  selectedValuesProp: 'value'
  selectedItemProp: 'value'
  searchObjProps: 'name,variable'

# The UI View for the filter state, handles autocomplete for phrases
class DiscoverFilterView extends Backbone.View
  @implements TemplateView
  initialize: ->
        @model = window.discoverFilter or= new DiscoverFilterModel
        @initTemplate '#discover-filter'

  addFilterTag: (tag) =>
        @model.addPhrase $(tag).find ':last-child'
        tag

  removeFilterTag: (tag) =>
        @model.removePhrase $(tag).find ':last-child'
        tag

  allObjects: ->
        window.Treatments.toJSON().concat window.Instruments.toJSON()

  render: =>
        @renderTemplate()
        @

  finalize: =>
        $(@el).find('#discover-filter-input').autoSuggest @allObjects(), $(discoverSuggestDefaults).extend
                selectionAdded: @addFilterTag
                selectionRemoved: @removeFilterTag

# -----------------------------
#   NLP filter list according to selected tags
# -----------------------------

# Views for individual models
# - requires @model and @parent to be valid
class DiscoverItemView extends Backbone.View
  className: 'discover-list-item'
  initTemplate: ->
        @template = @parent.getTemplate @model

  initialize: ->
        @parent = @options.parent
        @initTemplate()
        @

  events:
        'mouseover': 'inspect'
        'click': 'zoom'

  render: =>
        $(@el).html @template @model.toJSON()
        @

  inspect: ->
        window.socialView.setContext @model

  zoom: ->



# The UI View for the filtered object set
class DiscoverListView extends Backbone.View
  # SETUP
  className: 'discover-list'
  subviews:
        experiment: '#experiment-list-view'
        treatment: '#treatment-list-view'
        instrument: '#instrument-list-view'

  compileSubviews: ->
        results = {}
        _.map @subviews, (id, type) ->
                results[type] = Handlebars.compile $(id).html()
        results

  getTemplate: (model) ->
        type = model.get 'type'
        @templates[type]

  initialize: ->
        window.listView = @ #debug
        @model.bind('change', @updateView)
        @templates = @compileSubviews()
        @items = []
        @updateView()
        @

  # Handling changes
  getResults: (limit) ->
        filter = @model.get 'query'
#        alert 'filter using: ' + filter
        window.Treatments.models

  asItemView: (model) =>
        new DiscoverItemView
                model: model
                parent: this

  updateView: =>
        @items = _.map(@getResults(8), @asItemView)
        @render()

  render: =>
        $(@el).empty()
        $(@el).append view.render().el for view in @items
        @

  finalize: ->
        view.delegateEvents() for view in @items


# ----------------------------------------
#   Main browser window (rarely refreshed)
# ----------------------------------------
class DiscoverApp extends Backbone.View
  @implements TemplateView, SwitchPane
  initialize: ->
        @filterView = new DiscoverFilterView
        @listView = new DiscoverListView
                model: @filterView.model
        @

  render: =>
        $(@el).empty()
        $(@el).append '<h1>Discover</h1>'
        $(@el).append @filterView.render().el
        $(@el).append @listView.render().el
        @filterView.finalize()
        @listView.finalize()
        @

#        $(@el).append @discoverList.render().el
#"/api/suggest/discover/", discoverSuggestDefaults

# ---------------------------------
#   Trial Viewer
# ---------------------------------
class TrialView extends Backbone.View
  @implements TemplateView
  initialize: (exp) ->
        @initTemplate '#trial-list-view'

  render: =>
        @renderTemplate()

  finalize: ->
        @delegateEvents()

class TrialApp extends Backbone.View
  @implements TemplateView, SwitchPane
  newTrial: (trial) ->
        new TrialView
                model: trial

  initialize: (exp) ->
        @views = window.MyTrials.map @newTrial
        window.TrialApp = @
        @

  render: =>
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
  initialize: (id) ->
        @el = $('#social' ? id)[0]
        @template = Handlebars.compile $('#comment-short-view').html()
        @render()

  setContext: (model) ->
        @model = model
        @render()

  renderComment: (comment) ->
        $(@el).append @template comment

  render: =>
        $(@el).empty()
        $(@el).append '<h1>Discussion</h1>'
        if @model
                @renderComment c for c in @model.comments() or [] when @model.comments
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
                'discover': new DiscoverApp
                        el: $('#discoverApp').first()
                'admin': new AdminApp
                        el: $('#adminApp').first()

  # Routing
  routes:
        '/app/:app/*path': 'activate'
        '/app/:app':       'activate'

  # Activate an app
  activate: (name, path) =>
        console.log 'Activating ' + name + ' with ' + path
        if typeof path == 'undefined'
            window.mainMenu.setMenu "app/" + name
        else
            window.mainMenu.setMenu "app/" + name + "/" + path
        pane = @switcher.get(name)
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
        @

   events:
        'click a': 'activate'

   setCurrent: (link) ->
        @$('li.current').removeClass('current')
        $(link).parent().addClass 'current'

   setMenu: (base, path) =>
        if path
           link = $("a[href='#{@root}/#{base}/#{path}']")
        else
           link = $("a[href='#{@root}/#{base}']")
        @setCurrent link

   activate: (event) =>
        event.preventDefault()
        newLink = $(event.target)
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
   window.socialView = new SocialView '#social'
   window.appRouter = new AppRouter
   window.mainMenu = new MainMenu {elid: '#main-menu'}
   window.mainMenu.delegateEvents()
   match = Backbone.history.start
        pushState: true
        root: "/app"
   window.appRouter.navigate document.location.pathname, true
