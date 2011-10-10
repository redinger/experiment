#
# Routes, Application and Initialization for
#     InventHealth.com
#

#
# MODELS
#

# User Creds
# - Initialized
class User extends Backbone.Model
  username: -> @get('username')
  adminp: -> 'admin' in @get('permissions')


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


# Treatment Model
class Treatment extends Backbone.Model
  defaults:
        'tags': []
        'comments': []

  tag: (tag) =>
    @set 'tags': @get('tags').push(tag)

  comment: (comment) =>
    @set 'comments': @get('comments').push(comment)

  url: "/api/bone/treatment"

# Client UI State Models
class BrowserFilter extends Backbone.Model
   defaults:
        'type': 'all'
        'sort': ['title', 'asc']

class BrowserModel extends Backbone.Model
   defaults:
        'state': 'list'


# VIEWS

# SubView for the filter
class BrowserFilterView extends Backbone.View
  initialize: ->

# AppView for full page apps

class AppView extends Backbone.View
  visiblep: =>
        if @el.is(':visible')
           true
        else
           false

  hide: =>
        if not @visiblep()
           return null
        promise = $.Deferred (dfd) => @el.fadeOut('fast', dfd.resolve)
        promise.promise()

  show: =>
        if @visiblep()
           return null
        promise = $.Deferred (dfd) => @el.fadeIn('fast', dfd.resolve)
        promise.promise()

  dispatch: (path) =>
        @render()


# Home page
class HomeApp extends AppView
  initialize: ->
        @el = $('#homeApp')
        @

  render: ->
        $(@el).append '<h1>Hello Home Page</h1>'
        @

# Main browser window (rarely refreshed)
class BrowserApp extends AppView
  initialize: ->
        @el = $('#browserApp')
        @filter = new BrowserFilter
#        @filterView = new BrowserFilterView @filter
#        @objects = new BrowserCollection
        @mode = 'summary'
        @

  render: =>
        $(@el).append '<h1>Hello Browser</h1>'
        $(@el).append @filterView.render() unless $('#filterView')
        $(@el).append @browser.render() unless $('#browser')
        @

# Main Menu View Handler
class MainMenu extends Backbone.View
   menuids: ["main-menu"]

   initialize: (id) ->
        @el = $(id)
        ddsmoothmenu.init
                'mainmenuid': 'main-menu'
                'orientation': 'v'
                'classname': 'main-menu'
                'contentsource': 'markup'
        link.bind 'click', @activate for link in @$('a')

   activate: (event) =>
        alert('got click')
        AppRouter.navigate "/app/" + .html()
        @


#
# AppRouter dispatches the main URL, creates and/or activates the
#   App and allows the app to configure itself
#
class AppRouter extends Backbone.Router
  panes: []

  # Utilities
  hideAll: =>
        pane.hide() for pane in @panes

  # Routing
  routes:
        'app/:app/*path': 'activate'
        'app/:app':       'activate'

  activate: (app, path) ->
        console.log 'activating ' + app
        app = switch app
          when 'home' then @home = new HomeApp unless @home
          when 'browse' then @browser = new BrowserApp unless @browser
          when 'admin' then @admin = new AdminApp unless @admin
        if app
          console.log 'dispatching ' + path
          @panes.push(app)
          app.dispatch(path)
          if not app.visiblep()
             @hideAll()
             app.show()
        else
           @hideAll()
           $('#errorApp').show()


# After the page is loaded, we have the skeleton in place
# and just need to dispatch to the app we need based on the
# rest of the URL.  Any other top-level initialization can be
# done here too

$(document).ready ->
   window.AppRouter = new AppRouter '#app-pane'
   window.MainMenu = new MainMenu '#main-menu'
   Backbone.history.start
        pushState: true

