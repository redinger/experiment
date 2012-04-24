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
        height = options.height or 100
        @cfg =
          w: width
          h: height
          render:
            xaxis: false
            yaxis: true
            series: true
            path: true
            meanOffsets: false
          defaults:
            glyph: "glyph"
            pointClass: "point"
            timeaxis: "rule"
          title: options.title
          margin: 20

        # Render default
        @fetchData Date.parse('2 months ago'), Date.parse('1 month ago')
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
        if @data? and @data.series?
          QIChart.runChart '#' + @id, @data, @cfg
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
              title: model.instrument.get('variable')
            view
        , @
        _.last(@views).setXaxisView true
        @

      updateTimeline: =>
        @dates = $('#rangepicker').val()
        [start, end] = _.map @dates.split(" - "), Date.parse
        _.each @views, (view) ->
          view.fetchData start, end
        @

      render: ->
        # Render tracker Header
        @$el.append @template
          range: @dates
        @$('#rangepicker').daterangepicker
          arrows: true
          onChange: _.debounce(@updateTimeline, 200)
          rangeStartTitle: 'Timeline Start Date'
          rangeEndTitle: 'Timeline End Date'
          earliestDate: ''

        # Render trackers
        _.map @views, (view) ->
            @$el.append view.render().el
        , @

        # Tooltip support
        $('svg circle.glyph').tooltip
          manual: 'true'
        @

    return Timeline
