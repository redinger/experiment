define ['jquery', 'models/infra', 'models/core', 'models/user', 'views/widgets', 'views/journal', 'views/timeline', 'views/trial', 'use!Handlebars', 'use!D3time', 'use!BackboneFormsBS', 'use!BackboneFormsEditors', 'use!jQueryDatePicker', 'use!Moment', 'views/events' ],
  ($, Infra, Core, User, Widgets, Journal, Timeline, Trial) ->

# Control Chart
# ------------------------------------------

    class ControlChart extends Backbone.View
      initialize: (options) ->
        @trials = Core.theUser.trials

      render: ->
        @$el.append
        @

# Summary View
# ---------------------------------
    class Overview extends Backbone.View
      initialize: (options) ->
        @router = options.router
        @page = Infra.templateLoader.getTemplate 'overview-page'
#        @help = Infra.templateLoader.getTemplate 'dashboard-help'
        @$el.html @page {}

        @trialFrame = new Trial.Frame
          collection: Core.theUser.trials
        @trialFrame.on 'view', @view, @

        @calendar = new Widgets.Calendar
          url: "/api/calendar/user"
        @calendar.on 'view', @view, @

        @journals = Core.theUser.journals
        now = new Date()
        @journalView = new Journal.View
          model: null
        @

      view: (tab, args) ->
        @router.navigate "#{ tab }/#{ args }",
          trigger: true

      render: ->
        @$('#summary-pane').html @trialFrame.render().el
        @$('#calendar-pane').html @calendar.render().el
        @$('#journal-pane').html @journalView.render().el
#        @$('#help-pane').html @help {}
        @

# Event Log
# ---------------------------------
    class EventView extends Backbone.View
      attributes:
        class: "event"

      initialize: (options) ->
        @template = Infra.templateLoader.getTemplate 'event-view'
        @model.on 'change', @render
        @

      render: =>
        @$el.html @template @model.toTemplateJSON()
        @

      events:
        'click .edit-event': 'editEvent'
        'click .submit-event': 'submitEvent'
        'click .view-timeline': 'viewTimeline'

      viewTimeline: =>
        window.Dashboard.router.navigate '/timeline',
          trigger: true

      editEvent: =>
        @$('.event-view').append "<div class='event-editor'><input type='text' class='event-data'></input> <button type='button' class='submit-event btn'>Submit</button><button type='button' class='submit-event btn'>Cancel</button></div>"

      submitEvent: =>
        entry = @$('.event-data').val()
        $.ajax "/api/events/submit",
           type: "POST"
           data:
             userid: @model.user.id
             instid: @model.instrument.id
             date: @model.get('start')
             text: "#{ @model.get('sms-prefix') } #{ entry }"
           context: @
           success: (evt) ->
             if evt
               @model.set evt
               @model.set
                 error: null
             else
               @model.set
                 error: "There was a problem recording your entry"

    periodHeader = (target, date) ->
      date = moment(date).format('dddd, MMMM, Do YYYY')
      $(target).append("<div class='dateheader'><h3>" + date + "</h3></div>")

    class EventLog extends Backbone.View
      initialize: (options) ->
        @template = Infra.templateLoader.getTemplate 'event-log'
        @dates = "1 day ago" + " - " + "tomorrow"
        @router = options.router
        @router.on 'route:selectTab', @onSwitch, @
        @

      # If we have a valid date argument on switch, update the time
      # control and fetch new data
      onSwitch: (tab, data) =>
        if tab is 'eventlog' and data?
           date = moment(data, 'YYYY-MM-DD')
           if not _.isNaN(date._d.hours())
              start = date.format('YYYY-MM-DD')
              end = date.add('days', 3).format('YYYY-MM-DD')
              range =  start + " - " + end
              daterange = @$('#eventrange')
              if daterange.length is 0
                @dates = range
              else
                daterange.val range
                @updateResults()
              @router.navigate 'eventlog',
                trigger: false
                replace: true

      updateResults: () =>
        @dates = $('#eventrange').val()
        [start, end] = _.map @dates.split(" - "), Date.parse
        @fetchResults(start, end)

      fetchResults: (start, end) ->
        $.ajax "/api/events/fetch",
          data:
            start: start.toISOString()
            end: end.toISOString()
          context: @
          success: (groups) =>
            # Remove old views, if any
            if @groups
              _.each @groups, (group) ->
                _.each group.views, (view) ->
                  view.remove()
                , @
              , @
            @$('#eventlist').empty() # pick up the headers too
            # Create views for the events in each group
            @groups = _.map groups, (group) ->
              group.views = _.map group.events, (datum) ->
                new EventView
                  model: new (Backbone.ReferenceCache.lookupConstructor(datum.type))(datum)
              , @
              group
            , @
            # Update the document
            @renderResults()

      renderResults: ->
        listDiv = @$('#eventlist')
        if @groups? and not _.isEmpty @groups
          _.each @groups, (group) ->
            periodHeader(listDiv, group.date)
            _.each group.views, (view) ->
              listDiv.append view.render().el
            , @
          , @
        else
          listDiv.html "<h3>No Events Found</h3>"

      render: ->
        @$el.html @template {range: @dates}
        @$('#eventrange').daterangepicker
          arrows: true
          autoSize: true
          onChange: _.debounce(@updateResults, 200)
          rangeStartTitle: 'Events Start'
          rangeEndTitle: 'Events End'
          earliestDate: '1 month ago'
        @updateResults()
        @

# Dashboard Navigation
# -------------------------------------------
    class DashboardRouter extends Backbone.Router
      initialize: (options = {}) ->
          @default = options.default
          @args = []
          @

      routes:
          ':tab/*path': 'selectTab'
          ':tab':       'selectTab'
          '':           'selectDefault'

      selectDefault: (data) =>
          @navigate @default, {trigger: true, replace: true} if @default?


# Dashboard Application
# --------------------------------------------
#

    getStudies = () ->
      prefs = Core.theUser.get('preferences')
      studies = []
      if prefs['study2-consented']?
        studies.push $("<li class='abs pull-right'><a href='/study2'>Self-Experiment Study</a></li>")
      if prefs['study1-consented']?
        studies.push $("<li class='abs pull-right'><a href='/study1'>Authoring Study</a></li>")
      studies

    class Dashboard extends Backbone.View
      initialize: (options) ->
          # Navigation
          @router = new DashboardRouter
              default: '/overview'

          @navbar = new Widgets.NavBar
              el: $('.subnav-fixed-top')
              router: @router

          # Shameless hack to add links to nav bar
          studies = getStudies()
          $('.nav-inner ul').append study for study in studies

          # Dashboard Tabs
          @tabs = {}
          @tabs.overview = new Overview
              el: $('#overview')
              router: @router
          @tabs.timeline = new Timeline
              el: $('#timeline')
              router: @router
          @tabs.eventlog = new EventLog
              el: $('#eventlog')
              router: @router
          @tabs.journal = new Journal.Page
              collection: Core.theUser.journals
              el: $('#journal')
              router: @router

      render: (options) ->
          _.map @tabs, (viz, name) ->
              viz.render()

    $(document).ready ->
      window.Dashboard = new Dashboard()

      Backbone.history.start
            root: '/dashboard/'
            pushState: true

      window.Dashboard.render()

      $('.popover-link').popover
        placement: 'bottom'


