# Base Models and Collections

class ExModel extends Backbone.Model
  initialize: (models, options) ->

  comment: (comment) =>
    @set 'comments': @get('comments').push(comment)



class Treatment extends ExModel
  tag: (tag) =>
    @set 'tags': @get('tags').push(tag)


# Gets a collection and column headers
# Renders into a table template
#
# class TableView extends Backbone.View
#   initialize: (id, columns, coll) ->
#     @columns = columns
#     @model = coll
#     $(id).dataTable
#        'aaData': @model # array of arrays
#        'aaColumns': @coll # array of column objects
#     @

#   render: =>

class Instrument extends ExModel
  initialize: (attrs) ->
     @set implementedp: false
     @set attrs
     @


