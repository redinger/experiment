(function() {
  define(['models/infra', 'models/core', 'views/journal', 'use!Handlebars', 'use!D3time', 'use!BackboneFormsBS', 'use!BackboneFormsEditors'], function(Infra, Models, JournalView) {
    return 1;
  });
  /* */
  /*
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
  
      ## ------------------------
      ## Charts
      ## ------------------------
  
      renderTrackerChart = (id, instrument, start, extra, options) ->
              options = $(options).extend
                      chart:
                              type: 'line'
                              renderTo: id
              $('#'+id).html('<h1>Fetching Tracking Chart</h1>')
              $.getJSON "/api/charts/tracker",
                      inst: instrument.get 'id',
                      (config) ->
                              config = $.extend config, options
                              App.chart_config = config
                              new Highcharts.Chart config
  
  
  
  
  
  # -------------------------------------------
  # APPLICATION PANES
  # -------------------------------------------
  
      # +++++++++++++++++++++
      # Dashboard Page
      # +++++++++++++++++++++
  
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
              tabpath = $(e.target).attr('href').split('/')
              tabpath = _.rest(tabpath, 2).join('/')
              App.router.navigate tabpath, true
  
  
  # -------------------------------------------------
  # APPLICATION
  #   Create main app panes, handle url dispatch, and frame elements
  # -------------------------------------------------
  
      #
      # AppRouter dispatches the main URL, creates and/or activates the
      #
      class DashboardRouter extends Backbone.Router
        # Routing
        routes:
              ':dashboard/*path': 'select'
              ':dashboard':       'select'
  
        # Activate an app
        select: (pname, path) =>
              @trigger "route:selectApp", pname, path
  
      $(document).ready ->
         # Instantiate the SocialView
         # App.socialView = new SocialView {el: $('#share-pane')}
  
         # Create the main app body
         App.start()
  
         # Initialize navigation, resolve URL
         match = Backbone.history.start
              root: '/app/'
              pushState: true
              silent: false
  */
}).call(this);
