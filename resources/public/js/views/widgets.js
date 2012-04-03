(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; }, __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype;
    child.prototype = new ctor;
    child.__super__ = parent.prototype;
    return child;
  };
  define(['models/infra', 'use!Backbone'], function(Infra) {
    var PaneSwitcher, SwitchPane, TemplateView;
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
      TemplateView: TemplateView,
      SwitchPane: SwitchPane,
      PaneSwitcher: PaneSwitcher
    };
  });
}).call(this);
