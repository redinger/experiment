define ['jquery', 'use!Backbone'],
  ($, Backbone) ->

# ----------------------------------------
# Language Extensions
# ----------------------------------------

    # ## Coffeescript Class Mixins
    _implements = (classes...) ->
        for klass in classes
            # static properties
            for prop of klass
                @[prop] = klass[prop]
            # prototype properties
            for prop of klass.prototype
                    getter = klass::__lookupGetter__(prop)
                    setter = klass::__lookupSetter__(prop)

                    if getter || setter
                        @::__defineGetter__(prop, getter) if getter
                        @::__defineSetter__(prop, setter) if setter
                    else
                        @::[prop] = klass::[prop]
        return this

    # Add 'implements' to global function prototype for @implements in class defs
    if Object.defineProperty
        Object.defineProperty Function.prototype, "implements", value : _implements
    else
        Function::implements = _implements


# ----------------------------------------
# Backbone Extensions
# ------------------------------------------

    # ## Backbone.Embedded
    # - Lazy loading (if only attributes are id, type)
    # - References to shared objects
    # - Submodel support

    # Save references we're going to override
    _parse = Backbone.Model.prototype.parse
    _initialize = Backbone.Model.prototype.initialize
    _set = Backbone.Model.prototype.set

    # Singleton reference store
    Backbone.Cache = ->
       @_collections = [];

    # Simple registration protocol
    _.extend Backbone.Cache.prototype, Backbone.Events,
      resolve: (type, id) ->
        collection = _collections[type] or {}
        if collection and collection[id]
            return collection[id]
        else
            instance = new type
                id: id
            instance._loaded = false
            return @register type, instance

      register: (type, instance) ->
        instance.bind 'destroy', @unregister, @
        @_collections[type][instance.id] = instance
        instance

      unregister: ( instance ) ->
        instance.unbind 'destroy', @unregister
        type = instance.constructor
        delete collections[type][instance.id]

    # Add Embedded Functionality to Backbone.Model
    Cache = Backbone.Cache

    _importers:
      # Just return the reference, ok if overwriting
      reference: (attr,model,type,ref) ->
        Cache.resolve type, ref[1]

      # These collections need to be of type RefCollection
      # so add/del serializes and update the server's array of
      # references
      references: (attr, coll, type, ref_array) ->
        coll || coll = new type
        coll.reset _.map ref_array, (ref) =>
            Cache.resolve type, ref[1]
        coll.parent = @
        coll.location = attr
        coll

      # Parse into existing or new model if data is provided
      submodel: (attr, model, type, attrs) ->
        model = new type if not model
        if attrs_array and attrs.length > 0
            model.reset attrs
            model.loaded = true
        else
            model.reset
            model.loaded = false
        model.parent = @
        model.location = attr
        model

      # Parse into existing or new collection if data is provided
      submodels: (attr, coll, type, attrs_array) ->
        coll = new type if not coll
        if attrs_array and attrs_array.length > 0
            model.reset attrs_array
            model.loaded = true
        else
            model.reset
            model.loaded = false
        coll.parent = @
        coll.location = attr
        model

    # ## Extend Backbone.Model to override parsing
    _.extend Backbone.Model.prototype,
      _loaded = true

      # Extract embedded values as direct model attributes
      parse: (response) ->
            attributes = _parse(response)
            if @embedded
                _.each @embedded, (record, attr) ->
                    type = record[0]
                    importer = record[1]
                    value = attributes[attr]
                    @[attr] = _importers[importer].call(@,attr,@[attr],type,value)
                    delete attributes[attr]
            attributes

       # Seatbelts so we don't try to get or set embedded models
       get: (attr) ->
        if @embedded and @embedded[attr]
            throw new Error('Cannot get an embedded attribute')
        if _loaded is false
           @fetch()
        _get.call(model, attr)

      set: (attr, value) ->
        if @embedded and @embedded[attr]
            throw new Error('Cannot get an embedded attribute')
        if _loaded is false
           @fetch
              success: (model, response) ->
                _set.call(model, attr, value)


# Experiment Server Compatible Models
# -----------------------------------------

## Support PE Bone API and type naming scheme

# serverType: specifies the type tag for the model,
#   default if 'type' attribute is not present
# location: the parent's attribute containing this model
#    or collection, overrides serverType if present

    class Model extends Backbone.Model
      # Declarations for subclasses
      getLocation: ->
        if @location
            return @location
        else if @get 'type'
            return @get 'type'
        else if @serverType
            return @serverType
        else
            throw new Error('No server type provided for URL')

      getPath: ->
        local = "/{ @getLocation }/{ @get 'id' }"
        if @parent
          "{ @parent.getPath() }/{ local }"
        else
          local

      url: () ->
        "/api/root/{ @getPath() }"

    class SubModel extends Model
      url: () ->
        "/api/embed/{ @getPath() }"

    class Collection extends Backbone.Collection
      getServerType: ->
        if @serverType
            @serverType
        else if @get 'type'
            @get 'type'
        else
            throw new Error('No server type provided for Model')

      url: () ->
        "/api/bone/#{ type }"

    class SubCollection extends Backbone.Collection
      url: () ->
        ptype = @parent.getServerType()
        pid = @parent.id
        type = @getServerType()
        "/api/embed/#{ ptype }/#{ pid }/#{ type }"


# ------------------------------------------------------------
# View Widgets and Base Classes
# ------------------------------------------------------------

    #
    # TemplateView - default template support for Backbone.View apps
    #
    class TemplateView
      getTemplate: (name) ->
            try
              html = $(name).html()
              alert 'No template found for #{ name }' unless name
              Handlebars.compile html
            catch error
              alert "Error loading template #{ name } ... #{ error }"

      initTemplate: (name) ->
            @template = @getTemplate name
            @

      resolveModel: (model) ->
            if not model
               model = @model
            if not model
               model = new Backbone.Model({})
            if not model.toJSON
               model = new Backbone.Model(model)
            model

      renderTemplate: (model, template) ->
            model = @resolveModel model
            template or= @template
            @$el.html template model.toJSON()
            @

      inlineTemplate: (model, template) ->
            model = @resolveModel model
            template or= @template
            @$el.append template model.toJSON()


    #
    # SwitchPane - A view that can be managed by a container, such as the switcher
    #
    class SwitchPane
      visiblep: =>
            if @$el.is(':visible')
               true
            else
               false

      hidePane: =>
            if @visiblep()
    #           @$el.fadeOut('fast')
               @$el.hide()
    #        promise = $.Deferred (dfd) => @el.fadeOut('fast', dfd.resolve)
    #        promise.promise()

      showPane: =>
            if not @visiblep()
    #           @$el.fadeIn('fast')
               @$el.show()
    #        promise = $.Deferred (dfd) => @el.fadeIn('fast', dfd.resolve)
    #        promise.promise()

      dispatch: (path) =>
            @


    #
    # PaneSwitcher - Manages a set of AppView objects, only one of which
    #    should be displayed at a time
    #
    class PaneSwitcher extends Backbone.View
      # List of panes
      panes: {}
      initialize: ->
            @panes = @options.panes
            @

      render: =>
            @$el.empty()
            @$el.append pane.render().el for name,pane of @panes
            @

      # Switch panes
      hideOtherPanes: (target) ->
            if not target
               (pane.hidePane() if pane) for name, pane of @panes
            else if typeof target == 'string'
               (pane.hidePane() if pane and name != target) for own name, pane of @panes
            else
               (pane.hidePane() if pane and pane != target) for own name, pane of @panes

      switch: (name) =>
            @active = name
            pane = @getPane name
            if pane == null
              alert 'no pane for name ' + name
            @hideOtherPanes name
            pane.showPane()

      # Modify list
      addPane: (name, pane) ->
            @panes[name] = pane
            @trigger('switcher:add')
            @render()
            @

      removePane: (ref) ->
            if typeof ref == 'string'
               delete @panes[ref]
            else
               name = _.detect @panes, (name, pane) ->
                    if (pane == ref)
                            name
               delete @panes[name]
            @trigger('switcher:remove')
            @render()
            @

      getPane: (name) ->
            pane = @panes[name]
            if typeof pane == 'undefined'
               alert 'Pane not found for name ' + name
            pane

    # Return the map of classes
    Model: Model
    SubModel: SubModel
    Collection: Collection
    SubCollection: SubCollection
    TemplateView: TemplateView


