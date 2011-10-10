# CREATE Application for managing objects in

class CreateRouter extends Backbone.Router

  routes:
        'view/:type': 'review'
        'edit/:type/:id': 'edit'
        'remove/:type/:id': 'remove'

  review: (type) ->
        alert 'review objects'

  edit: (type, id) ->
        alert 'edit an object'

  remove: (type, id) ->
        alert 'remove an object'


