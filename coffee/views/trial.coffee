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
