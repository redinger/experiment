define ['models/infra', 'models/core', 'views/widgets', 'QIchart', 'use!D3time', 'use!BackboneFormsBS', 'use!BackboneFormsEditors', 'use!jQueryDatePicker' ],
  (Infra, Core, Widgets, QIChart) ->

    # Run chart view with dynamic loading
    # - Requires options.model, options.id, and options.cfg
    class RunChart extends Backbone.View
      className: 'qi-run-chart'

      initialize: (options) ->
        @$el.attr('clear', 'both')

        # Default configuration
        width = options.width or 900
        height = options.height or 120
        @cfg =
          w: width
          h: height
          render:
            xaxis: false
            yaxis: true
            series: true
          defaults:
            glyph: "glyph"
            chart: @id
            timeaxis: "rule"
          margin: 20

        # Render default
        @fetchData Date.parse('1 month ago'), Date.parse('today')
        @

      # Configure view
      setXaxisView: (value) ->
        @cfg.render.xaxis = value
        if @data?
           @render()

      # Update dataset
      fetchData: (start, end) ->
        $.ajax '/api/charts/tracker',
          data:
            start: start.toISOString()
            end: end.toISOString()
            inst: @model.instrument.id
          context: @
          success: (data) =>
            @data = data
            @render()
          spinner: false

      render: () ->
        @$el.empty()
        if @data?
          QIChart.runChart '#' + @id, @data, @cfg
        else
          @$el.html('<p>' + @model.instrument.get('variable') + '</p>')
        @


    # Top level Timeline View
    # ---------------------------------------------
    class Timeline extends Backbone.View
      initialize: ->
        # Header template and state
        @template = Infra.templateLoader.getTemplate 'timeline-header'
        @dates = "2 months ago" + " - " + "1 month ago"

        # Create tracker views
        @collection = Core.theUser.trackers
        @views = @collection.map (model) ->
            view = new RunChart
              model: model
              id: 'tracker-' + model.instrument.get('src')
            view
        , @
        _.last(@views).setXaxisView true
        @

      events:
        'change #rangepicker': 'updateTimeline'

      updateTimeline: =>
        @dates = $('#rangepicker').val()
        [start, end] = _.map @dates.split(" - "), Date.parse
        console.log [start, end]
        _.each @views, (view) ->
          view.fetchData start, end
        @

      render: ->
        # Render tracker Header
        @$el.append @template
          date: @dates
        @$('#rangepicker').daterangepicker
          arrows: true
          onChange: @updateTimeline
          rangeStartTitle: 'Timeline Start Date'
          rangeEndTitle: 'Timeline End Date'
          earliestDate: ''

        # Render trackers
        _.map @views, (view) ->
            @$el.append view.render().el
        , @
        @

    return Timeline
