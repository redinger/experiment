define ['models/infra', 'models/core', 'views/widgets', 'QIchart', 'use!D3time', 'use!BackboneFormsBS', 'use!BackboneFormsEditors', 'use!jQueryDatePicker' ],
  (Infra, Core, Widgets, QIChart) ->

    # Run chart view with dynamic loading
    # - Requires options.model, options.id, and options.cfg
    class RunChart extends Backbone.View
      className: 'qi-run-chart'

      initialize: (options) ->
        @$el.attr('clear', 'both')

        # Default configuration
        width = options.width or 850
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
            start: moment(start).format('YYYYMMDDTHHmmss.000Z')
            end: moment(end).format('YYYYMMDDTHHmmss.000Z')
            inst: @model.instrument.id
          context: @
          success: (data) =>
            @data = data
            @render()
          spinner: false

      render: () ->
        @$el.empty()
        if @data? and @data.series?
          QIChart.renderChart '#' + @id, @data, _.extend(@cfg, {title: @model.instrument.get('variable')})
        @


    # Top level Timeline View
    # ---------------------------------------------
    class Timeline extends Backbone.View
      initialize: ->
        # Header template and state
        @template = Infra.templateLoader.getTemplate 'timeline-header'
        @dates = "1 month ago - today"

        # Create tracker views
        @collection = Core.theUser.trackers
        @views = @collection.map (model) ->
            view = new RunChart
              model: model
              id: 'tracker-' + model.id
              title: model.instrument.get('variable')
            view
        , @
        @views = _.sortBy @views, (view) ->
          view.model.instrument.id
        if not _.isEmpty @views
          _.first(@views).setXaxisView 'top'
          _.last(@views).setXaxisView true
        @

      updateTimeline: =>
        dates = $('#timelinerange').val()
        @dates = dates if dates
        [start, end] = _.map @dates.split(" - "), Date.parse
        _.each @views, (view) ->
          view.fetchData start, end
        @

      render: ->
        # Render tracker Header
        @$el.html @template
          range: @dates
        @$('#timelinerange').daterangepicker
          arrows: true
          autoSize: true
          onChange: _.debounce(@updateTimeline, 200)
          rangeStartTitle: 'Timeline Start Date'
          rangeEndTitle: 'Timeline End Date'
          earliestDate: ''

        # Render trackers
        if _.isEmpty @views
          @$el.append "<h3 style='text-align:center;margin-left:auto;margin-right:auto;margin-top:100px;width:50em'><a href='/explore/search/query/show instruments/p1'>Find an Instrument to Track</a></h3>"
        else
          _.map @views, (view) ->
              @$el.append view.render().el
          , @
        @updateTimeline()

        # Tooltip support
        $('svg circle.glyph').tooltip
          manual: 'true'
        @

    return Timeline
