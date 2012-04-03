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
# Backbone.Embedded Extension
# ----------------------------------------
#
# - Submodels: manage sub-fields of a document which maintain an
#   embedding hierarchy
# - References: a poor-mans Bootstrap.Relational, pass a [type-tag, id]
#   field which allows you to have a class of objects be stored in a cache.
# - Global caching of referenced objects
# - Global server type tag registration for instantiating objects on load.
# - Lazy loading of references: set id and type and fetch in the background
#   or batch fetch later (if have API to support).  We can create
#   singleton referred objects lazily.  For now we background load them.
#

    # Save methods we're going to override
    _set = Backbone.Model.prototype.set
    _get = Backbone.Model.prototype.get
    _toJSONModel = Backbone.Model.prototype.toJSON

    # Singleton reference store
    class ReferenceCache
      constructor: ->
           @types = {} # assoc: types[name] => constructors
           @instances = {} # assoc: instances[id] => model
           @

    _.extend ReferenceCache.prototype,
      registerTypes: (map) ->
          _.extend(@types, map)

      lookupConstructor: (type) ->
          if _.isString(type) then @types[type] else type

      resolve: (type, id, options = {lazy: false}) ->
          if not (id? or type?)
            throw new Error('ReferenceCache.resolve requires valid type and id')
          instance = @instances[id]
          if not instance
            instance = new(@lookupConstructor(type))({id: id})
            instance._refType = type
            instance._embedLocation = () -> null
            instance._embedParent = null
            if not options.lazy and not instance._loaded
              instance._loaded = instance.fetch().complete( ->
                  instance._loaded = true
              ).fail( ->
                  console.log 'failed to load:'
                  console.log instance
              )
            else if options.lazy
              instance._loaded = false
              throw new Error('Lazy loading not implemented')
            @register instance
          return instance

      register: (instance) ->
          instance.on 'destroy', @unregister
          @instances[instance.id] = instance
          instance

      unregister: ( instance ) ->
          instance.off 'destroy', @unregister
          delete @instances[instance.id]

    # Create a singleton cache instance to track instances
    Backbone.ReferenceCache = new ReferenceCache

# # Add Embedded Model Functionality to Backbone.Model

    # ## Extend Backbone.Model to override parsing
    _.extend Backbone.Model.prototype,
      _loaded = true # default is explictly managed, or loaded
      _importers:
          # Just return the reference, ok if overwriting
          reference: (attr,model,conType,ref) ->
              if _.isArray ref
                [servType, id] = ref
                Backbone.ReferenceCache.resolve servType, id, {lazy: false}
              else
                ref

          # These collections need to be of type RefCollection
          # so add/del serializes and update the server's array of
          # references
          references: (attr, coll, conType, ref_array) ->
              coll ?= new(Backbone.ReferenceCache.lookupConstructor(conType))
              coll.reset _.map ref_array, (ref) ->
                if _.isArray ref
                   [servType, id] = ref
                   Backbone.ReferenceCache.resolve servType, id, {lazy: false}
                else
                   ref
              coll._embedParent = this
              coll._embedLocation = attr
              coll._referenceCollection = true
              coll

          # Parse into existing or new model if data is provided
          submodel: (attr, model, type, attrs) ->
              model ?= new(Backbone.ReferenceCache.lookupConstructor(type))
              if attrs and attrs.length > 0
                model.set attrs
              else
                model.clear
                model._loaded = false
              model._embedParent = @
              model._embedLocation = () -> attr
              model

          # Parse into existing or new collection if data is provided
          submodels: (attr, coll, type, attrs_array) ->
              coll ?= new(Backbone.ReferenceCache.lookupConstructor(type))
              coll._embedParent = this
              coll._embedLocation = () -> attr
              if not _.isEmpty attrs_array
                importModel = (attrs) ->
                    if not _.isObject attrs
                        throw new Error('Invalid submodel attributes')
                    model = new(coll.model)
                    model.set attrs
                    model._embedParent = coll
                    model._embedLocation = () ->
                        if @id then @id else null
                    if not model.id?
                        throw new Error('Cannot import embedded models without id')
                    model
                coll.reset _.map _.values(attrs_array), importModel, @
              else
                coll.reset()
                coll._loaded = false
              coll

      # ## New Model Utilities

      # Is Lazy Loading in-process or complete? (holds a deferred if loading)
      isLoaded: ->
          @_loaded

      # Get the embedding path for this object (sequences of nested models)
      # returns chained array of objects from root to parent
      getEmbedPath: () ->
          if @_embedParent then @_embedParent.getEmbedPath().push @ else _.chain([@])

      # Return a server-friendly reference for this object
      # (Only root objects are referencable for now)
      asReference: () ->
          if @getEmbedPath().value()[0] is @
             [@_refType, @id]
          else
             throw new Error('Cannot return a reference for an embedded model')

      # ## Override default model behavior
      _setEmbedded: (attr, value) ->
          [importer, conType] = @embedded[attr]
          if not importer
             throw new Error('Trying to set non-embedded attribute ' + attr)
          @[attr] = @_importers[importer].call(@,attr,@[attr],conType,value)
          @

      set: (attr, value, options) ->
          if not @embedded?
             return _set.apply(@,arguments)
          if _.isObject attr
             _.each attr, (val,key) ->
                if @embedded[key]
                   @_setEmbedded key, val
                else
                   _set.call(this, key, val, options)
             , @
          else if @embedded[attr]
             @_setEmbedded attr, value
          else
             _set.apply(@,arguments)

      get: (attr) ->
          if @embedded and @embedded[attr] and @[attr]
             return @[attr]
          if not @_loaded and not attr is 'id' and not attr is 'type'
             throw new Error('Object is not loaded')
          _get.call(@, attr)

      toJSON: (options) ->
          json = _toJSONModel.call(@, options)
          _.each @embedded, (record, attr) ->
                 [exporter] = record
                 if exporter is 'reference' and @[attr]
                    json[attr] = @[attr].asReference()
          , @
          json


    # Extensions to Backbone.Collection for Embedded Models
    __prepareModel = Backbone.Collection.prototype._prepareModel
    _toJSONColl = Backbone.Collection.prototype.toJSON

    _.extend Backbone.Collection.prototype,
      # Get the embedding path for this object (sequences of nested models),
      # returns chained array of objects from root to parent
      getEmbedPath: () ->
          if @_embedParent then @_embedParent.getEmbedPath().push @ else _.chain([@])

      toJSON: (options) ->
          if @_referenceCollection
             @models.map (model) ->
                model.asReference()
          else
             _toJSONColl.call(@, options)

      # When adding a model, maintain the parent reference
      # if we're an embedded collection
      _prepareModel: (model, options) ->
          model = __prepareModel.call(@, model, options)
          if @_embedParent
            model._embedParent ?= @
            model._embedLocation = () ->
                if @id then @id else null
          model

# Specific Implementation of Submodel API
# -----------------------------------------

# ### Support for PersonalExperiments backend API
#
# Assumes Backbone.Embedded
#
# serverType: specify the type tag for the model,
#   default to 'type' attribute if it is not present.  Return it
#   in JSON requests if not provided
#

    class Model extends Backbone.Model
      # Declarations for subclasses
      getServerType: ->
          if @serverType
            @serverType
          else if @get 'type'
            @get 'type'
          else
            throw new Error('No server type provided for Model')

      # Default submodel URLs
      url: ->
          if @_embedParent
            [root, embeds...] = @getEmbedPath().value()
            locations = _.map embeds, (obj) ->
              obj._embedLocation()
            if @isNew()
              "/api/embed/#{ root.getServerType() }/#{ root.id }/#{ _.initial(locations).join('/') }"
            else
              "/api/embed/#{ root.getServerType() }/#{ root.id }/#{ locations.join('/') }"
          else
            "/api/root/#{ @getServerType() }/#{ @id }"

      # Make sure we always serialize our model type, particularly for new
      toJSON: (options) ->
          json = super options
          json.type = @getServerType() if not json.type
          json

    class Collection extends Backbone.Collection
      getServerType: ->
          @model.prototype.getServerType()

      url: ->
          if @_embedParent
            [root, embeds...] = @getEmbedPath().value()
            locations = _.map embeds, (obj) ->
                obj._embedLocation()
            "/api/embed/coll/#{ root.getServerType() }/#{ root.id }/#{ locations.join('/') }"
          else
            "/api/root/#{ @model.getServerType() }"

    # Return the map of useful classes
    Model: Model
    Collection: Collection
    ReferenceCache: ReferenceCache



