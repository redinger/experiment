#Backbone = require './backbone'

##
## Box layout test model
##

# Model

class ConfigModel extends Backbone.Model
   initialize: ->
        @set 'color': 'blue', 'width': '100', 'height': '100'
        @fetch()
   configBox: (color, dim) =>
        @set 'color': color, 'width': dim, 'height': dim
   url: "/api/bone/configmodel"

# View

class ConfigInputView extends Backbone.View
   initialize: ->
        @model.view = @
        @model.bind 'change', @render

   events:
        'keyup #color-input': 'updateConfig',
        'keyup #width-input': 'updateConfig',
        'click #save-forms': 'saveModel'
        'click #load-forms': 'loadModel'

   # Update form when object updated
   render: () =>
        $('#color-input').val(@model.get('color'))
        $('#width-input').val(@model.get('width'))

   updateConfig: (e) =>
        @model.configBox $('#color-input').val(), $('#width-input').val()

   saveModel: (e) =>
        @model.save()

   loadModel: (e) =>
        @model.fetch()


class ColorBoxView extends Backbone.View
   tagName: 'li'
   initialize: ->
        @template = $('#color-box-template')
        @model.bind 'change', @render
        @model.view = @

   render: () =>
        json = @model.toJSON()
        $(@el).html @template.mustache @model.toJSON()
        return @

# App Controller

class AppView extends Backbone.View
   el: $('#config-app')
   initialize: ->
        model = new ConfigModel
        color_input = new ConfigInputView 'el': $('#config-input'), 'model': model
        for x in [1..3]
           view = new ColorBoxView {"model": model}
           $('#color-boxes').append view.render().el

$(document).ready ->
        window.App = new AppView

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

