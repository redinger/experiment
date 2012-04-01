(function() {
  var __slice = Array.prototype.slice, __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; }, __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype;
    child.prototype = new ctor;
    child.__super__ = parent.prototype;
    return child;
  };
  define(['jquery', 'use!Backbone'], function($, Backbone) {
    var Cache, Collection, Model, PaneSwitcher, SubCollection, SubModel, SwitchPane, TemplateView, _implements, _initialize, _loaded, _parse, _set;
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
    _parse = Backbone.Model.prototype.parse;
    _initialize = Backbone.Model.prototype.initialize;
    _set = Backbone.Model.prototype.set;
    Backbone.Cache = function() {
      return this._collections = [];
    };
    _.extend(Backbone.Cache.prototype, Backbone.Events, {
      resolve: function(type, id) {
        var collection, instance;
        collection = _collections[type] || {};
        if (collection && collection[id]) {
          return collection[id];
        } else {
          instance = new type({
            id: id
          });
          instance._loaded = false;
          return this.register(type, instance);
        }
      },
      register: function(type, instance) {
        instance.bind('destroy', this.unregister, this);
        this._collections[type][instance.id] = instance;
        return instance;
      },
      unregister: function(instance) {
        var type;
        instance.unbind('destroy', this.unregister);
        type = instance.constructor;
        return delete collections[type][instance.id];
      }
    });
    Cache = Backbone.Cache;
    ({
      _importers: {
        reference: function(attr, model, type, ref) {
          return Cache.resolve(type, ref[1]);
        },
        references: function(attr, coll, type, ref_array) {
          coll || (coll = new type);
          coll.reset(_.map(ref_array, __bind(function(ref) {
            return Cache.resolve(type, ref[1]);
          }, this)));
          coll.parent = this;
          coll.location = attr;
          return coll;
        },
        submodel: function(attr, model, type, attrs) {
          if (!model) {
            model = new type;
          }
          if (attrs_array && attrs.length > 0) {
            model.reset(attrs);
            model.loaded = true;
          } else {
            model.reset;
            model.loaded = false;
          }
          model.parent = this;
          model.location = attr;
          return model;
        },
        submodels: function(attr, coll, type, attrs_array) {
          if (!coll) {
            coll = new type;
          }
          if (attrs_array && attrs_array.length > 0) {
            model.reset(attrs_array);
            model.loaded = true;
          } else {
            model.reset;
            model.loaded = false;
          }
          coll.parent = this;
          coll.location = attr;
          return model;
        }
      }
    });
    _.extend(Backbone.Model.prototype, _loaded = true, {
      parse: function(response) {
        var attributes;
        attributes = _parse(response);
        if (this.embedded) {
          _.each(this.embedded, function(record, attr) {
            var importer, type, value;
            type = record[0];
            importer = record[1];
            value = attributes[attr];
            this[attr] = _importers[importer].call(this, attr, this[attr], type, value);
            return delete attributes[attr];
          });
        }
        return attributes;
      },
      get: function(attr) {
        if (this.embedded && this.embedded[attr]) {
          throw new Error('Cannot get an embedded attribute');
        }
        if (_loaded === false) {
          this.fetch();
        }
        return _get.call(model, attr);
      },
      set: function(attr, value) {
        if (this.embedded && this.embedded[attr]) {
          throw new Error('Cannot get an embedded attribute');
        }
        if (_loaded === false) {
          return this.fetch({
            success: function(model, response) {
              return _set.call(model, attr, value);
            }
          });
        }
      }
    });
    Model = (function() {
      __extends(Model, Backbone.Model);
      function Model() {
        Model.__super__.constructor.apply(this, arguments);
      }
      Model.prototype.getLocation = function() {
        if (this.location) {
          return this.location;
        } else if (this.get('type')) {
          return this.get('type');
        } else if (this.serverType) {
          return this.serverType;
        } else {
          throw new Error('No server type provided for URL');
        }
      };
      Model.prototype.getPath = function() {
        var local;
        local = "/{ @getLocation }/{ @get 'id' }";
        if (this.parent) {
          return "{ @parent.getPath() }/{ local }";
        } else {
          return local;
        }
      };
      Model.prototype.url = function() {
        return "/api/root/{ @getPath() }";
      };
      return Model;
    })();
    SubModel = (function() {
      __extends(SubModel, Model);
      function SubModel() {
        SubModel.__super__.constructor.apply(this, arguments);
      }
      SubModel.prototype.url = function() {
        return "/api/embed/{ @getPath() }";
      };
      return SubModel;
    })();
    Collection = (function() {
      __extends(Collection, Backbone.Collection);
      function Collection() {
        Collection.__super__.constructor.apply(this, arguments);
      }
      Collection.prototype.getServerType = function() {
        if (this.serverType) {
          return this.serverType;
        } else if (this.get('type')) {
          return this.get('type');
        } else {
          throw new Error('No server type provided for Model');
        }
      };
      Collection.prototype.url = function() {
        return "/api/bone/" + type;
      };
      return Collection;
    })();
    SubCollection = (function() {
      __extends(SubCollection, Backbone.Collection);
      function SubCollection() {
        SubCollection.__super__.constructor.apply(this, arguments);
      }
      SubCollection.prototype.url = function() {
        var pid, ptype, type;
        ptype = this.parent.getServerType();
        pid = this.parent.id;
        type = this.getServerType();
        return "/api/embed/" + ptype + "/" + pid + "/" + type;
      };
      return SubCollection;
    })();
    TemplateView = (function() {
      function TemplateView() {}
      TemplateView.prototype.getTemplate = function(name) {
        var html;
        try {
          html = $(name).html();
          if (!name) {
            alert('No template found for #{ name }');
          }
          return Handlebars.compile(html);
        } catch (error) {
          return alert("Error loading template " + name + " ... " + error);
        }
      };
      TemplateView.prototype.initTemplate = function(name) {
        this.template = this.getTemplate(name);
        return this;
      };
      TemplateView.prototype.resolveModel = function(model) {
        if (!model) {
          model = this.model;
        }
        if (!model) {
          model = new Backbone.Model({});
        }
        if (!model.toJSON) {
          model = new Backbone.Model(model);
        }
        return model;
      };
      TemplateView.prototype.renderTemplate = function(model, template) {
        model = this.resolveModel(model);
        template || (template = this.template);
        this.$el.html(template(model.toJSON()));
        return this;
      };
      TemplateView.prototype.inlineTemplate = function(model, template) {
        model = this.resolveModel(model);
        template || (template = this.template);
        return this.$el.append(template(model.toJSON()));
      };
      return TemplateView;
    })();
    SwitchPane = (function() {
      function SwitchPane() {
        this.dispatch = __bind(this.dispatch, this);
        this.showPane = __bind(this.showPane, this);
        this.hidePane = __bind(this.hidePane, this);
        this.visiblep = __bind(this.visiblep, this);
      }
      SwitchPane.prototype.visiblep = function() {
        if (this.$el.is(':visible')) {
          return true;
        } else {
          return false;
        }
      };
      SwitchPane.prototype.hidePane = function() {
        if (this.visiblep()) {
          return this.$el.hide();
        }
      };
      SwitchPane.prototype.showPane = function() {
        if (!this.visiblep()) {
          return this.$el.show();
        }
      };
      SwitchPane.prototype.dispatch = function(path) {
        return this;
      };
      return SwitchPane;
    })();
    PaneSwitcher = (function() {
      __extends(PaneSwitcher, Backbone.View);
      function PaneSwitcher() {
        this["switch"] = __bind(this["switch"], this);
        this.render = __bind(this.render, this);
        PaneSwitcher.__super__.constructor.apply(this, arguments);
      }
      PaneSwitcher.prototype.panes = {};
      PaneSwitcher.prototype.initialize = function() {
        this.panes = this.options.panes;
        return this;
      };
      PaneSwitcher.prototype.render = function() {
        var name, pane, _ref;
        this.$el.empty();
        _ref = this.panes;
        for (name in _ref) {
          pane = _ref[name];
          this.$el.append(pane.render().el);
        }
        return this;
      };
      PaneSwitcher.prototype.hideOtherPanes = function(target) {
        var name, pane, _ref, _ref2, _ref3, _results, _results2, _results3;
        if (!target) {
          _ref = this.panes;
          _results = [];
          for (name in _ref) {
            pane = _ref[name];
            _results.push((pane ? pane.hidePane() : void 0));
          }
          return _results;
        } else if (typeof target === 'string') {
          _ref2 = this.panes;
          _results2 = [];
          for (name in _ref2) {
            if (!__hasProp.call(_ref2, name)) continue;
            pane = _ref2[name];
            _results2.push((pane && name !== target ? pane.hidePane() : void 0));
          }
          return _results2;
        } else {
          _ref3 = this.panes;
          _results3 = [];
          for (name in _ref3) {
            if (!__hasProp.call(_ref3, name)) continue;
            pane = _ref3[name];
            _results3.push((pane && pane !== target ? pane.hidePane() : void 0));
          }
          return _results3;
        }
      };
      PaneSwitcher.prototype["switch"] = function(name) {
        var pane;
        this.active = name;
        pane = this.getPane(name);
        if (pane === null) {
          alert('no pane for name ' + name);
        }
        this.hideOtherPanes(name);
        return pane.showPane();
      };
      PaneSwitcher.prototype.addPane = function(name, pane) {
        this.panes[name] = pane;
        this.trigger('switcher:add');
        this.render();
        return this;
      };
      PaneSwitcher.prototype.removePane = function(ref) {
        var name;
        if (typeof ref === 'string') {
          delete this.panes[ref];
        } else {
          name = _.detect(this.panes, function(name, pane) {
            if (pane === ref) {
              return name;
            }
          });
          delete this.panes[name];
        }
        this.trigger('switcher:remove');
        this.render();
        return this;
      };
      PaneSwitcher.prototype.getPane = function(name) {
        var pane;
        pane = this.panes[name];
        if (typeof pane === 'undefined') {
          alert('Pane not found for name ' + name);
        }
        return pane;
      };
      return PaneSwitcher;
    })();
    return {
      Model: Model,
      SubModel: SubModel,
      Collection: Collection,
      SubCollection: SubCollection,
      TemplateView: TemplateView
    };
  });
}).call(this);
