define ['models/infra', 'models/core', 'views/widgets', 'QIchart', 'use!D3time', 'use!BackboneFormsBS', 'use!BackboneFormsEditors', 'use!jQueryDatePicker' ],
  (Infra, Core, Widgets, QIChart) ->

    # Control chart with dynamic loading

    class ControlChart extends Backbone.View
      initialize: (options) ->
        if @model.get('type') is not 'trial'
          alert 'Control chart init error'
        start = moment().subtract('days', 30)
        end = moment().add('days', 30)
        @fetchData start, end
        @cfg =
          w: 800
          h: 200
          render:
            xaxis: true
            yaxis: true
            series: true
            path: true
            meanOffsets: false
          defaults:
            glyph: "glyph"
            pointClass: "point"
            timeaxis: "rule"
          title: @model.experiment.get('title')
          margin: 20
        @

      fetchData: (start, end) ->
        if @data?
          @renderChart()
        else
          $.ajax '/api/charts/trial',
            data:
              id: @model.id
              start: start.format('YYYYMMDDTHHmmss.000Z')
              end: end.format('YYYYMMDDTHHmmss.000Z')
            context: @
            success: (data) =>
              if data? and data.series?
                @data = data
                @renderChart()
            spinner: false

      redraw: ->
        @$el.html('')
        @renderChart @data if @data?

      renderChart: () =>
        QIChart.renderChart @$el, @data, @cfg

      render: ->
        @

    # ---------------------------------
    # Trial Summary Pane
    # ---------------------------------
    class TrialPane extends Backbone.View
      attributes:
        class: 'trial-pane'

      initialize: (options) ->
          @template = Infra.templateLoader.getTemplate 'trial-view-header'
          @chart = new ControlChart
            id: @model.cid
            model: @model
          @model.on 'change', @render, @
          @model.experiment.on 'change', @render, @
          @

      update: ->
          @chart.remove()
          @$el.html @template @model.toTemplateJSON()
          @$el.append @chart.render().el
          @chart.redraw()
          @trigger 'update'
          @

      render: ->
          @delegateEvents()
          @$el.html @template @model.toTemplateJSON()
          @$el.append @chart.render().el
          @trigger 'update'
          @

      events:
          'click .pause': 'pause'
          'click .resume': 'resume'
          'click .cancel': 'cancel'
          'click .archive': 'archive'

      notify: (e) =>
          console.log e

      pause: (e) =>
          e.preventDefault()
          status = @model.get('status')
          if status is 'active'
             @model.save
                status: 'paused'
          else
             alert 'Error in trial handling, contact administrator'

      resume: (e) =>
          e.preventDefault()
          status = @model.get('status')
          if status is 'paused'
             @model.save
                status: 'active'
          else
             alert 'Error in trial handling, contact administrator'


      cancel: =>
          # start abandoned flow

      archive: =>
          # If not completed, complete

      complete: =>
          # Completion flow

    # ----------------------------------
    # Trial Wrapper Frame
    # ----------------------------------

    class TrialFrame extends Backbone.View
      attributes:
        class: 'trial-frame'

      initialize: (options) ->
        @template = Infra.templateLoader.getTemplate 'trial-view-frame'
        @collection.on 'reset add remove', @configureViews, @
        @configureViews()
        @

      configureViews: ->
        if @views
          _.each @views, (view) -> view.remove()
        @views = _.toArray @collection.map @newTrial, @
        @currentTrial = 0
        @render()
        @

      newTrial: (trial) ->
        trial = new TrialPane
          model: trial
        trial.on 'update', @updateTrialHeader, @
        trial

      render: ->
        @$el.html @template
           empty: _.isEmpty @views
        @renderTrial()

      renderTrial: =>
        console.log @currentTrial
        if @views[@currentTrial]?
          @$('#trial-pane-wrapper').html @views[@currentTrial].render().el
        @

      updateTrialHeader: =>
        @$('.trial-title-bar .button').removeClass 'disabled'
        if (@currentTrial + 1) >= @views.length
          @$('.trial-title-bar .next').addClass 'disabled'
        if @currentTrial <= 0
          @$('.trial-title-bar .prev').addClass 'disabled'

      renderInstruments: (experiment) ->
            mydiv = $("<div class='trial-measures'/>")
            @$el.append mydiv
            mydiv.append '<h2>Tracking Instruments</h2>'
            instruments = experiment.get('instruments')
            data = {instruments: _.map(instruments, (x) -> x.toTemplateJSON())}
            mydiv.append @instTableTemplate data

      events:
        'click button.next': 'nextTrial'
        'click button.prev': 'prevTrial'

      nextTrial: (event) =>
        if not $(event.currentTarget).hasClass('disabled')
          @currentTrial = @currentTrial + 1
          @renderTrial()
          @updateTrialHeader()

      prevTrial: (event) =>
        if not $(event.currentTarget).hasClass('disabled')
          @currentTrial = @currentTrial - 1
          @renderTrial()
          @updateTrialHeader()

    # NAMESPACE

    Pane: TrialPane
    Frame: TrialFrame
    ControlChart: ControlChart