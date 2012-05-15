define ['models/infra', 'models/core', 'models/user', 'views/common', 'views/widgets', 'use!Handlebars', 'use!BackboneFormBS', 'use!BackboneFormsEditors'],
  (Infra, Core, User, Common, Widgets) ->
    genJitter = (f) ->
      f _.range(0, 180, 15)

    timeToString = (time) ->
      mins = if time.min < 10 then "0" + time.min else time.min
      str = time.hour + ":" + mins
      if time.pm then str = str + "pm"
      str

    dailyProcess = (sched) ->
      _.each sched.times, (time) ->
        if time.pm then time.hour = time.hour + 12
      sched

    hourValid = (hour, formValues) ->
      if not (hour >= 0 and hour <= 23)
          type: 'hours'
          message: 'Hours must be between 0 and 23'
      else if (formValues.pm and hour > 11)
          type: 'hours'
          message: 'Hours must be 0-11 if using PM'
      else
          null

    minuteValid = (min, formValues) ->
      if not (min >= 0 and min <= 59)
          type: 'minutes'
          message: 'Minutes must be between 0 and 59'

    timesValid = (times, formValues) ->
      console.log times
      if times.length < 1
          type: 'times'
          message: 'Must choose at least one daily time for taking this measurement'

    dailySchema =
        stype:
          type: "Hidden"
        sms:
          title: "Enable SMS"
          type: "Checkbox"
          options: ["Yes"]
        times:
          title: "Sample Times"
          type: "List"
          listType: "Object"
          validators: [ timesValid ]
          subSchema:
            hour:
              type: "Number"
              title: "Hour"
              validators: [ hourValid ]
            min:
              type: "Number"
              title: "Minute"
              validators: [ minuteValid ]
            pm:
              type: "Checkbox"
              title: "pm?"
          itemToString: timeToString
        jitter:
          type: "Select"
          title: "Sample Time Jitter"
          options: genJitter
          help: "Will remind you at each selected time plus or minus this many minutes"

    dailyDefault =
        stype: 'daily'
        sms: [true]
        times: []
        jitter: 15

    weeklySchema =
        stype:
          type: 'Hidden'
        sms:
          title: 'Enable SMS'
          type: 'Checkbox'
        day:
          type: 'Select'
          title: 'Day of Week'
          options: [ {val: 0, label: 'Sunday'}, {val: 1, label: 'Monday'}, {val: 2, label: 'Tuesday'}, {val: 3, label: 'Wednesday'}, {val: 4, label: 'Thursday'}, {val: 5, label: 'Friday'}, {val: 6, label: 'Saturday'} ]
        times:
          title: "Sample Times"
          type: "List"
          listType: "Object"
          validators: [ timesValid ]
          subSchema:
            hour:
              type: "Number"
              title: "Hour"
              validators: [ hourValid ]
            min:
              type: "Number"
              title: "Minute"
              validators: [ minuteValid ]
            pm:
              type: "Checkbox"
              title: "pm?"
          itemToString: timeToString

    weeklyDefault =
        stype: 'weekly'
        sms: [true]
        day: 6
        times: []
        jitter: 15

    class ScheduleView extends Backbone.View
      attributes:
        class: 'ScheduleView'

      initialize: (options) ->
        @template = Infra.templateLoader.getTemplate 'scheduler-view'
        @parent = options.parent
        @callback = options.callback
        @schedule = new Core.Schedule
        @forms = {}
        @forms.daily = new Backbone.Form
          data: (if @schedule.stype is 'daily' then @schedule.toJSON() else dailyDefault)
          schema: dailySchema
          itemToString: (item) ->
        @forms.weekly = new Backbone.Form
          data: (if @schedule.stype is 'weekly' then @schedule.toJSON() else weeklyDefault)
          schema: weeklySchema
        @

      render: ->
        tabs =
          daily: @forms.daily?
          weekly: @forms.weekly?
        @$el.html @template tabs
        @$('#daily').append @forms.daily.render().el if @forms.daily?
        @$('#weekly').append @forms.weekly.render().el if @forms.weekly?
        @$('.tab-content .tab-pane:first').addClass('active')
        @$('#schedTab li:first').addClass('active')
        @

      events:
        'click a': 'selectTab'
        'click .accept': 'accept'
        'click .cancel': 'cancel'

      selectTab: (event) =>
        event.preventDefault()
        $(event.target).tab('show')

      accept: (event) =>
        event.preventDefault()
        active = @$('.tab-content .active').attr('id')
        form = @forms[active]
        result = form.getValue()

        if active is 'daily'
           result = dailyProcess result
        if active is 'weekly'
           result = dailyProcess result
        if result.jitter?
           result.jitter = parseInt result.jitter
        if result.day?
           result.day = parseInt result.day
        result = _.extend result,
          type: "schedule"

        @callback result
        @trigger 'nav:doView', @parent

      cancel: (event) =>
        event.preventDefault()
        @callback null if @callback?
        @trigger 'nav:doView', @parent

    configureTracker = (parent, instrument, handler) ->
      src = instrument.get('src')
      schedule = instrument.schedule
      if src is 'manual'
        view = new ScheduleView
          parent: parent
          schedule: schedule
          callback: handler
        parent.trigger 'nav:doView', view
      else if not src?
        Common.modalMessage.showMessage
          header: "Failure to track #{instrument.get('variable')}"
          message: "<p>There is a problem with this instrument and it is not ready for tracking</p>"
        null
      else
        Common.modalMessage.showMessage
          header: "Tracking #{instrument.get('variable')}"
          message: "<p>This instrument will automatically download data from the #{instrument.get('service')} service according to a pre-defined schedule.  At times the data may slightly lag the data that is available directly via the service.</p>"
          accept: "Track"
          reject: "Cancel"
          callback: (result) ->
            if result is 'accept'
              handler(null)
              if not Core.theUser.services.find( (service) ->
                   service.get('name') is instrument.get('service') )
                Common.modalMessage.showMessage
                  header: "Configure Service"
                  message: "Click Continue to configure the #{instrument.get('service')} service"
                  accept: "Continue"
                  reject: "Skip"
                  callback: (result) ->
                    if result is 'accept'
                      window.location.pathname = '/account/services'
        null

# ## EXPORT
    configureTrackerSchedule: configureTracker
    ScheduleView: ScheduleView
