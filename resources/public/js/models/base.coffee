(function() {
  var __slice = Array.prototype.slice, __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; }, __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype;
    child.prototype = new ctor;
    child.__super__ = parent.prototype;
    return child;
  };
  define(['use!Backbone'], function(Backbone) {
    var Model, ModelCollection, PaneSwitcher, SwitchPane, TemplateView, findModel, implements, resolveReference, returnUser;
    implements = function() {
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
        value: implements
      });
    } else {
      Function.prototype.implements = implements;
    }
    (function() {
      var _parse;
      _parse = Backbone.Model.prototype.parse;
      return _.extend(Backbone.Model.prototype, {
        initNestedCollections: function(nestedCollections) {
          this._nestedCollections = {};
          _.each(nestedCollections, __bind(function(theClass, key) {
            return this._nestedCollections[key] = new theClass([]);
          }, this));
          return this.resetNestedCollections();
        },
        parse: function(response) {
          var attributes;
          attributes = _parse(response);
          this.resetNestedCollections();
          return attributes;
        },
        resetNestedCollections: function() {
          return _.each(this._nestedCollections, __bind(function(collection, key) {
            var models;
            models = _.map(this.get(key), __bind(function(attributes) {
              var model;
              model = new collection.model();
              return model.set(model.parse(attributes));
            }, this));
            return collection.reset(models);
          }, this));
        },
        getNestedCollection: function(name) {
          return this._nestedCollections[name];
        }
      });
    })();
    findModel = function(models, id) {
      return models.find(__bind(function(mod) {
        return mod.id === id;
      }, this));
    };
    returnUser = function(id) {
      var user;
      if (id === App.User.get('id')) {
        return App.User;
      } else {
        user = findModel(App.Users, id);
        if (!user) {
          return alert("Found object reference to another user " + id);
        } else {
          return user;
        }
      }
    };
    resolveReference = function(ref) {
      if (ref) {
        if (ref ? !_.isArray(ref) : void 0) {
          alert("Bad Reference [" + ref + "]");
        }
        switch (ref[0]) {
          case 'experiment':
            return findModel(App.Experiments, ref[1]);
          case 'treatment':
            return findModel(App.Treatments, ref[1]);
          case 'instrument':
            return findModel(App.Instruments, ref[1]);
          case 'trial':
            return findModel(App.MyTrials, ref[1]);
          case 'tracker':
            return findModel(App.MyTrackers, ref[1]);
          case 'user':
            return returnUser(ref[1]);
          default:
            return alert("Reference not found [" + ref + "]");
        }
      }
    };
    Model = (function() {
      __extends(Model, Backbone.Model);
      function Model() {
        this.annotate = __bind(this.annotate, this);
        this.toJSON = __bind(this.toJSON, this);
        this.resolveRefs = __bind(this.resolveRefs, this);
        Model.__super__.constructor.apply(this, arguments);
      }
      Model.prototype.dbrefs = [];
      Model.prototype.resolveRefs = function() {
        var field, updates, value, _i, _len, _ref;
        updates = {};
        _ref = this.dbrefs;
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          field = _ref[_i];
          value = this.get(field);
          if (_.isArray(_.first(value))) {
            updates[field] = _.map(this.get(field), resolveReference);
          } else {
            updates[field] = resolveReference(this.get(field));
          }
        }
        return this.set(updates);
      };
      Model.prototype.url = function() {
        var id, type;
        type = this.get('type');
        id = this.get('id');
        return "/api/bone/" + type + "/" + id;
      };
      Model.prototype.toJSON = function() {
        var attrs, field, iter, model, _i, _len, _ref;
        attrs = Backbone.Model.prototype.toJSON.call(this);
        _ref = this.dbrefs;
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          field = _ref[_i];
          model = attrs[field];
          if (_.isArray(model)) {
            iter = function(model) {
              if (model) {
                return model.toJSON();
              }
            };
            if (model) {
              attrs[field] = _.map(model, iter);
            }
          } else {
            attrs[field] = attrs[field].toJSON();
          }
        }
        return attrs;
      };
      Model.prototype.annotate = function(type, text) {
        var id, mtype;
        id = this.get('id');
        mtype = this.get('type');
        $.post("/api/annotate/" + mtype + "/" + id + "/" + type, {
          text: text
        });
        return this.fetch();
      };
      return Model;
    })();
    ModelCollection = (function() {
      __extends(ModelCollection, Backbone.Collection);
      function ModelCollection() {
        this.resolveReferences = __bind(this.resolveReferences, this);
        ModelCollection.__super__.constructor.apply(this, arguments);
      }
      ModelCollection.prototype.resolveReferences = function() {
        return this.map(function(mod) {
          return mod.resolveRefs();
        });
      };
      return ModelCollection;
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
      implements: implements,
      Model: Model,
      Collection: ModelCollection,
      TemplateView: TemplateView
    };
  });
}).call(this);
