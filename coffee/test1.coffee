#Backbone = require './backbone'

##
## Box layout test model
##

# Model

initTemplate = (name) ->
        @template = Handlebars.compile $(name).html()

renderTemplate = () ->
        $(@el).html @template @model.first().toJSON()

class ConfigModel extends Backbone.Model
   defaults:
        'color': 'blue'
        'width': '100'
        'height': '100'
   configBox: (color, dim) =>
        @set 'color': color, 'width': dim, 'height': dim

class ConfigCollection extends Backbone.Collection
   model: ConfigModel
   urlRoot: "/api/bone/configmodel"

# View

class ConfigInputView extends Backbone.View
   initialize: ->
        @model.view = @
        @model.bind 'change', @render
        @model.bind 'reset', @render

   events:
        'keyup #color-input': 'updateConfig',
        'keyup #width-input': 'updateConfig',
        'click #save-forms': 'saveModel'
        'click #load-forms': 'loadModel'

   # Update form when object updated
   render: () =>
        if @model.first()
             $('#color-input').val(@model.first().get('color'))
             $('#width-input').val(@model.first().get('width'))

   updateConfig: (e) =>
        @model.first().configBox $('#color-input').val(), $('#width-input').val()

   saveModel: (e) =>
        @model.first().save()

   loadModel: (e) =>
        result = @model.fetch()

class ColorBoxView extends Backbone.View
   tagName: 'li'
   initialize: ->
        initTemplate.call this, '#color-box-template'
        @model.bind 'change', @render
        @model.bind 'reset', @render
        @model.view = @

   render: () =>
        if @model.first()
           renderTemplate.call this
        this

# App Controller

class AppView extends Backbone.View
   el: $('#config-app')
   initialize: ->
        @model = new ConfigCollection
        color_input = new ConfigInputView 'el': $('#config-input'), 'model': @model
        for x in [1..3]
           view = new ColorBoxView {"model": @model}
           $('#color-boxes').append view.render().el

$(document).ready ->
   window.App = new AppView
   try
      window.App.model.reset $.parseJSON $('#configmodel-bootstrap').html()
   catch error
      console.log "No bootstrapping found"
   unless window.App.model.first()
      window.App.model.add new ConfigModel
      window.App.model.trigger('reset')



#class ColorBoxCtrl extends Backbone.Router
#   initialize: ->
#        model = 10
#        model = new ConfigModel 'test': '12'
#        color_input = new ConfigInputView 'el': $('#config-input'), 'model': model
#        for x in [1..5]
#           view = new ColorBoxView {model: model}
#           $('#color-boxes').append view.render().el

#
# Another customer MVC setup
#

# class window.TreatmentView extends Backbone.View
#    initialize: ->
#         console.log this
#         @model.bind 'change:title', @changeTitle

#    events: 'click .title': 'handleTitleClick'

#    render: -> @el = @model.toJSON()

#    changeTitle: ->
#         @$('.title').text(@model.get('title'))

#    handleTitleClick:->
#         alert('you clicked the title: ' + @model.get('title'))

# view = new TreatmentView

# view.render()

