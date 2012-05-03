define ['jquery', 'use!Backbone', 'use!Handlebars'],
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
           @lazy = false
           @types = {} # assoc: types[name] => constructors
           @instances = {} # assoc: instances[id] => model
           @

      registerTypes: (map) ->
          _.extend(@types, map)

      lookupConstructor: (type) ->
          if _.isString(type) then @types[type] else type

      loadAll: () ->
          _.each @instances, (instance, id) ->
            if instance._loaded is false
               instance.fetch()

      importFromID: (id, target) ->
          string = $(id).html()
          attrs = $.parseJSON(string)
          @import attrs, target if attrs?

      import: (data, target) ->
          if _.isArray(data)
             if target? then throw new Error('cannot import array to target')
             _.map data, (attrs) ->
                @import.call(@,attrs,target)
             , @
          else if _.isObject(data)
             if target?
                target.set 'id', data.id
                target.set 'type', data.type
                @register target
                target.set data
                target
             else
                object = @resolve data.type, data.id, {lazy: @lazy, attrs: data}
                object.set data
                object

      resolve: (type, id, options = {lazy: @lazy}) ->
          id ?= options.attrs.id if options.attrs?
          type ?= options.attrs.type if options.attrs?
          if not (id?)
            throw new Error('ReferenceCache.resolve requires valid id for existing objects')
          instance = @instances[id]
          if not instance?
            if not (type?)
                throw new Error('ReferenceCache.resolve requires valid type and id for uncached objects')
            attrs = options.attrs or {type: type, id: id}
            instance = new(@lookupConstructor(type))(attrs)
            if options.attrs
               instance._loaded = true
            else
               instance._loaded = false
            @register instance, options
          instance

      register: (instance, options = {lazy: @lazy}) ->
          if @instances[instance.id]
             throw new Error('Cannot register over an existing object')

          # Reference state
          instance._refType = instance.get('type')
          instance._embedLocation = () -> null
          instance._embedParent = null

          # Configure lazy status
          if not options.lazy and ( _.values(instance.attributes).length <= 2 )
            instance._loading = instance.fetch().fail( ->
                console.log 'failed to load:'
                console.log instance
            )
          else if options.lazy
            instance._loaded = false
            if @lazy is not true
              throw new Error('Lazy loading not implemented')

          # Remove when destroyed
          instance.on 'destroy', @unregister, @
          @instances[instance.id] = instance
          instance

      unregister: ( instance ) ->
          instance.off 'destroy', @unregister, @
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
              coll._embedParent = @
              coll._embedLocation = () -> attr
              coll._referenceCollection = true
              coll

          # Parse into existing or new model if data is provided
          submodel: (attr, model, type, attrs) ->
              model ?= new(Backbone.ReferenceCache.lookupConstructor(type))
              if attrs and not _.isEmpty(attrs)
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
          # Set embedded value, trigger cascaded change events
          @[attr].off 'change', @_forceChange, @ if @[attr]?
          @[attr] = @_importers[importer].call(@,attr,@[attr],conType,value)
          if @.get('type') is not 'user'
             @[attr].on 'change', @_forceChange, @
          @

      _forceChange: (obj, options) ->
          if options? and options.triggered?
             if _.include(options.triggered, @)
                false
             else
                options.triggered.push @
                @trigger 'change', @, options
          else
             @trigger 'change', @, {triggered: [@]}

      set: (attr, value, options = {}) ->
          if _.isObject attr
             delete @['_loading'] if @_loading
             @_loaded = true
          if not @embedded?
             return _set.apply(@,arguments)
          # Set all embedded objects
          if _.isObject attr
             _.each attr, (val,key) ->
                if @embedded[key]
                   @_setEmbedded key, val
                   delete attr[key]
                   if not options.silent
                       @trigger 'change:' + attr, @, @get attr, options
             , @
             if _.isEmpty(attr)
                if not options.silent
                    @trigger 'change', @, options
             else
                _set.call(this, attr, options)
          # Set embedded object instance
          else if @embedded[attr]
             @_setEmbedded attr, value
             if not options.silent
                @trigger 'change', @, options
                @trigger 'change:' + attr, @, @get attr, options
             @
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

      toTemplateJSON: (options) ->
          json = _toJSONModel.call(@, options)
          _.each @embedded, (record, attr) ->
                 [exporter] = record
                 if exporter is 'reference' and @[attr]
                    json[attr] = @[attr].toTemplateJSON()
          , @
          json

      type: ->
        @get('type')

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

# Template Loader
# -----------------------------------------
#
# Support lazy loading of templates if they aren't all pre-rendered
# into the DOM.
    class TemplateLoader
      constructor: (options) ->
          @templateUrl = '/api/templates/'
          @templateCache = {}
          @

      fetchUrl: (id) ->
          if _.isFunction @templateUrl
            @templateUrl {id: id}
          else
            @templateUrl + id


      getTemplate: (id, options = {lazy: false}) ->
          if @templateCache[id]?
            @templateCache[id]
          else if $('#' + id).length > 0
            @templateCache[id] = Handlebars.compile $('#' + id).html()
          else
            deferred = $.ajax
                          url: @fetchUrl(id)
                          async: options.lazy
                          context: @
                          success: (data) ->
                            @templateCache[id] = Handlebars.compile data
            if options.lazy
               deferred
            else
               @templateCache[id]


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

# Common Mixins for Models
# ---------------------------

    class Taggable
      addTagString: (tagstr) =>
        if tagstr?
          tags = tagstr.split(',')
          tags = _.map tags, $.trim
          @addTags(tags)

      addTags: (tags) ->
        if tags.length > 0
          alltags = _.union @get('tags'), tags
          @save {tags: alltags}

      remTags: (tags) ->
        if tags.length > 0
          alltags = _.difference @get('tags'), tags
          if alltags.length > 0
            @save {tags: alltags}

    class Commentable


# Global Setup
# ---------------------------

    # ## Setup Underscore templates for trivial templating
    #    Use Handlebars.clj for more complex variants
    _.templateSettings =
        interpolate: /\{\{(.+?)\}\}/g

    # ## Return the map of useful classes
    Model: Model
    Taggable: Taggable
    Commentable: Commentable
    Collection: Collection
    ReferenceCache: ReferenceCache
    templateLoader: new TemplateLoader('/api/templates/{{ id }}')



