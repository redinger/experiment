(function() {
  var __slice = Array.prototype.slice, __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype;
    child.prototype = new ctor;
    child.__super__ = parent.prototype;
    return child;
  };
  define(['jquery', 'use!Backbone'], function($, Backbone) {
    var Collection, Model, ReferenceCache, __prepareModel, _get, _implements, _loaded, _set, _toJSONColl, _toJSONModel;
    _implements = function() {
      var classes, getter, klass, prop, setter, _i, _len;
      classes = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
      for (_i = 0, _len = classes.length; _i < _len; _i++) {
        klass = classes[_i];
        for (prop in klass) {
          this[prop] = klass[prop];
        }
        for (prop in klass.prototype) {
          getter = klass.prototype.__lookupGetter__(prop);
          setter = klass.prototype.__lookupSetter__(prop);
          if (getter || setter) {
            if (getter) {
              this.prototype.__defineGetter__(prop, getter);
            }
            if (setter) {
              this.prototype.__defineSetter__(prop, setter);
            }
          } else {
            this.prototype[prop] = klass.prototype[prop];
          }
        }
      }
      return this;
    };
    if (Object.defineProperty) {
      Object.defineProperty(Function.prototype, "implements", {
        value: _implements
      });
    } else {
      Function.prototype.implements = _implements;
    }
    _set = Backbone.Model.prototype.set;
    _get = Backbone.Model.prototype.get;
    _toJSONModel = Backbone.Model.prototype.toJSON;
    ReferenceCache = (function() {
      function ReferenceCache() {
        this.types = {};
        this.instances = {};
        this;
      }
      return ReferenceCache;
    })();
    _.extend(ReferenceCache.prototype, {
      registerTypes: function(map) {
        return _.extend(this.types, map);
      },
      lookupConstructor: function(type) {
        if (_.isString(type)) {
          return this.types[type];
        } else {
          return type;
        }
      },
      resolve: function(type, id, options) {
        var instance;
        if (options == null) {
          options = {
            lazy: false
          };
        }
        if (!((id != null) || (type != null))) {
          throw new Error('ReferenceCache.resolve requires valid type and id');
        }
        instance = this.instances[id];
        if (!instance) {
          instance = new (this.lookupConstructor(type))({
            id: id
          });
          instance._refType = type;
          instance._embedLocation = function() {
            return null;
          };
          instance._embedParent = null;
          if (!options.lazy && !instance._loaded) {
            instance._loaded = instance.fetch().complete(function() {
              return instance._loaded = true;
            }).fail(function() {
              console.log('failed to load:');
              return console.log(instance);
            });
          } else if (options.lazy) {
            instance._loaded = false;
            throw new Error('Lazy loading not implemented');
          }
          this.register(instance);
        }
        return instance;
      },
      register: function(instance) {
        instance.on('destroy', this.unregister);
        this.instances[instance.id] = instance;
        return instance;
      },
      unregister: function(instance) {
        instance.off('destroy', this.unregister);
        return delete this.instances[instance.id];
      }
    });
    Backbone.ReferenceCache = new ReferenceCache;
    _.extend(Backbone.Model.prototype, _loaded = true, {
      _importers: {
        reference: function(attr, model, conType, ref) {
          var id, servType;
          if (_.isArray(ref)) {
            servType = ref[0], id = ref[1];
            return Backbone.ReferenceCache.resolve(servType, id, {
              lazy: false
            });
          } else {
            return ref;
          }
        },
        references: function(attr, coll, conType, ref_array) {
          if (coll == null) {
            coll = new (Backbone.ReferenceCache.lookupConstructor(conType));
          }
          coll.reset(_.map(ref_array, function(ref) {
            var id, servType;
            if (_.isArray(ref)) {
              servType = ref[0], id = ref[1];
              return Backbone.ReferenceCache.resolve(servType, id, {
                lazy: false
              });
            } else {
              return ref;
            }
          }));
          coll._embedParent = this;
          coll._embedLocation = attr;
          coll._referenceCollection = true;
          return coll;
        },
        submodel: function(attr, model, type, attrs) {
          if (model == null) {
            model = new (Backbone.ReferenceCache.lookupConstructor(type));
          }
          if (attrs && attrs.length > 0) {
            model.set(attrs);
          } else {
            model.clear;
            model._loaded = false;
          }
          model._embedParent = this;
          model._embedLocation = function() {
            return attr;
          };
          return model;
        },
        submodels: function(attr, coll, type, attrs_array) {
          var importModel;
          if (coll == null) {
            coll = new (Backbone.ReferenceCache.lookupConstructor(type));
          }
          coll._embedParent = this;
          coll._embedLocation = function() {
            return attr;
          };
          if (!_.isEmpty(attrs_array)) {
            importModel = function(attrs) {
              var model;
              if (!_.isObject(attrs)) {
                throw new Error('Invalid submodel attributes');
              }
              model = new coll.model;
              model.set(attrs);
              model._embedParent = coll;
              model._embedLocation = function() {
                if (this.id) {
                  return this.id;
                } else {
                  return null;
                }
              };
              if (!(model.id != null)) {
                throw new Error('Cannot import embedded models without id');
              }
              return model;
            };
            coll.reset(_.map(_.values(attrs_array), importModel, this));
          } else {
            coll.reset();
            coll._loaded = false;
          }
          return coll;
        }
      },
      isLoaded: function() {
        return this._loaded;
      },
      getEmbedPath: function() {
        if (this._embedParent) {
          return this._embedParent.getEmbedPath().push(this);
        } else {
          return _.chain([this]);
        }
      },
      asReference: function() {
        if (this.getEmbedPath().value()[0] === this) {
          return [this._refType, this.id];
        } else {
          throw new Error('Cannot return a reference for an embedded model');
        }
      },
      _setEmbedded: function(attr, value) {
        var conType, importer, _ref;
        _ref = this.embedded[attr], importer = _ref[0], conType = _ref[1];
        if (!importer) {
          throw new Error('Trying to set non-embedded attribute ' + attr);
        }
        this[attr] = this._importers[importer].call(this, attr, this[attr], conType, value);
        return this;
      },
      set: function(attr, value, options) {
        if (!(this.embedded != null)) {
          return _set.apply(this, arguments);
        }
        if (_.isObject(attr)) {
          return _.each(attr, function(val, key) {
            if (this.embedded[key]) {
              return this._setEmbedded(key, val);
            } else {
              return _set.call(this, key, val, options);
            }
          }, this);
        } else if (this.embedded[attr]) {
          return this._setEmbedded(attr, value);
        } else {
          return _set.apply(this, arguments);
        }
      },
      get: function(attr) {
        if (this.embedded && this.embedded[attr] && this[attr]) {
          return this[attr];
        }
        if (!this._loaded && !attr === 'id' && !attr === 'type') {
          throw new Error('Object is not loaded');
        }
        return _get.call(this, attr);
      },
      toJSON: function(options) {
        var json;
        json = _toJSONModel.call(this, options);
        _.each(this.embedded, function(record, attr) {
          var exporter;
          exporter = record[0];
          if (exporter === 'reference' && this[attr]) {
            return json[attr] = this[attr].asReference();
          }
        }, this);
        return json;
      }
    });
    __prepareModel = Backbone.Collection.prototype._prepareModel;
    _toJSONColl = Backbone.Collection.prototype.toJSON;
    _.extend(Backbone.Collection.prototype, {
      getEmbedPath: function() {
        if (this._embedParent) {
          return this._embedParent.getEmbedPath().push(this);
        } else {
          return _.chain([this]);
        }
      },
      toJSON: function(options) {
        if (this._referenceCollection) {
          return this.models.map(function(model) {
            return model.asReference();
          });
        } else {
          return _toJSONColl.call(this, options);
        }
      },
      _prepareModel: function(model, options) {
        var _ref;
        model = __prepareModel.call(this, model, options);
        if (this._embedParent) {
          if ((_ref = model._embedParent) == null) {
            model._embedParent = this;
          }
          model._embedLocation = function() {
            if (this.id) {
              return this.id;
            } else {
              return null;
            }
          };
        }
        return model;
      }
    });
    Model = (function() {
      __extends(Model, Backbone.Model);
      function Model() {
        Model.__super__.constructor.apply(this, arguments);
      }
      Model.prototype.getServerType = function() {
        if (this.serverType) {
          return this.serverType;
        } else if (this.get('type')) {
          return this.get('type');
        } else {
          throw new Error('No server type provided for Model');
        }
      };
      Model.prototype.url = function() {
        var embeds, locations, root, _ref;
        if (this._embedParent) {
          _ref = this.getEmbedPath().value(), root = _ref[0], embeds = 2 <= _ref.length ? __slice.call(_ref, 1) : [];
          locations = _.map(embeds, function(obj) {
            return obj._embedLocation();
          });
          if (this.isNew()) {
            return "/api/embed/" + (root.getServerType()) + "/" + root.id + "/" + (_.initial(locations).join('/'));
          } else {
            return "/api/embed/" + (root.getServerType()) + "/" + root.id + "/" + (locations.join('/'));
          }
        } else {
          return "/api/root/" + (this.getServerType()) + "/" + this.id;
        }
      };
      Model.prototype.toJSON = function(options) {
        var json;
        json = Model.__super__.toJSON.call(this, options);
        if (!json.type) {
          json.type = this.getServerType();
        }
        return json;
      };
      return Model;
    })();
    Collection = (function() {
      __extends(Collection, Backbone.Collection);
      function Collection() {
        Collection.__super__.constructor.apply(this, arguments);
      }
      Collection.prototype.getServerType = function() {
        return this.model.prototype.getServerType();
      };
      Collection.prototype.url = function() {
        var embeds, locations, root, _ref;
        if (this._embedParent) {
          _ref = this.getEmbedPath().value(), root = _ref[0], embeds = 2 <= _ref.length ? __slice.call(_ref, 1) : [];
          locations = _.map(embeds, function(obj) {
            return obj._embedLocation();
          });
          return "/api/embed/coll/" + (root.getServerType()) + "/" + root.id + "/" + (locations.join('/'));
        } else {
          return "/api/root/" + (this.model.getServerType());
        }
      };
      return Collection;
    })();
    return {
      Model: Model,
      Collection: Collection,
      ReferenceCache: ReferenceCache
    };
  });
}).call(this);
