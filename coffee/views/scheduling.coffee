define ['models/infra', 'models/core', 'models/user', 'views/common', 'views/widgets', 'use!Handlebars', 'use!BackboneFormBS', 'use!BackboneFormsEditors'],
  (Infra, Core, User, Common, Widgets) ->

    class ScheduleView extends Backbone.Model
      initialize: (options) ->

    configure = (instrument) ->
      schedule = instrument.schedule
      if not schedule?
        console.log 'no schedule'
      if schedule? and schedule.type is 'service'
        Common.modalMessage.showMessage
          header: "Tracking #{instrument.variable}"
          message: "<p>This instrument will automatically download data from the #{instrument.service} service according to a pre-defined schedule.  At times the data may lag slightly the data that is available directly via the service."
          callback: ->
            if service is not 'manual'
              if Core.theUser.services.find( (service) ->
                   service.get('name') is instrument.get('service') )
                Common.modalMessage.showMessage
                  header: "Configure Service"
                  message: "Click OK to configure the #{instrument.get('service')} service"
                  callback: ->
                    alert 'navigate to services view'
      else if schedule.type is ''
        alert 'setup schedule config form'
      else
        alert "I don't know how to handle this instrument"

# ## EXPORT
    configureSchedule: configure
    ScheduleView: ScheduleView
