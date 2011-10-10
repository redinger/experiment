(function() {
  var AppView, ColorBoxView, ConfigCollection, ConfigInputView, ConfigModel, initTemplate, renderTemplate;
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; }, __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype;
    child.prototype = new ctor;
    child.__super__ = parent.prototype;
    return child;
  };
  initTemplate = function(name) {
    return this.template = Handlebars.compile($(name).html());
  };
  renderTemplate = function() {
    return $(this.el).html(this.template(this.model.first().toJSON()));
  };
  ConfigModel = (function() {
    __extends(ConfigModel, Backbone.Model);
    function ConfigModel() {
      this.configBox = __bind(this.configBox, this);
      ConfigModel.__super__.constructor.apply(this, arguments);
    }
    ConfigModel.prototype.defaults = {
      'color': 'blue',
      'width': '100',
      'height': '100'
    };
    ConfigModel.prototype.configBox = function(color, dim) {
      return this.set({
        'color': color,
        'width': dim,
        'height': dim
      });
    };
    return ConfigModel;
  })();
  ConfigCollection = (function() {
    __extends(ConfigCollection, Backbone.Collection);
    function ConfigCollection() {
      ConfigCollection.__super__.constructor.apply(this, arguments);
    }
    ConfigCollection.prototype.model = ConfigModel;
    ConfigCollection.prototype.urlRoot = "/api/bone/configmodel";
    return ConfigCollection;
  })();
  ConfigInputView = (function() {
    __extends(ConfigInputView, Backbone.View);
    function ConfigInputView() {
      this.loadModel = __bind(this.loadModel, this);
      this.saveModel = __bind(this.saveModel, this);
      this.updateConfig = __bind(this.updateConfig, this);
      this.render = __bind(this.render, this);
      ConfigInputView.__super__.constructor.apply(this, arguments);
    }
    ConfigInputView.prototype.initialize = function() {
      this.model.view = this;
      this.model.bind('change', this.render);
      return this.model.bind('reset', this.render);
    };
    ConfigInputView.prototype.events = {
      'keyup #color-input': 'updateConfig',
      'keyup #width-input': 'updateConfig',
      'click #save-forms': 'saveModel',
      'click #load-forms': 'loadModel'
    };
    ConfigInputView.prototype.render = function() {
      if (this.model.first()) {
        $('#color-input').val(this.model.first().get('color'));
        return $('#width-input').val(this.model.first().get('width'));
      }
    };
    ConfigInputView.prototype.updateConfig = function(e) {
      return this.model.first().configBox($('#color-input').val(), $('#width-input').val());
    };
    ConfigInputView.prototype.saveModel = function(e) {
      return this.model.first().save();
    };
    ConfigInputView.prototype.loadModel = function(e) {
      var result;
      return result = this.model.fetch();
    };
    return ConfigInputView;
  })();
  ColorBoxView = (function() {
    __extends(ColorBoxView, Backbone.View);
    function ColorBoxView() {
      this.render = __bind(this.render, this);
      ColorBoxView.__super__.constructor.apply(this, arguments);
    }
    ColorBoxView.prototype.tagName = 'li';
    ColorBoxView.prototype.initialize = function() {
      initTemplate.call(this, '#color-box-template');
      this.model.bind('change', this.render);
      this.model.bind('reset', this.render);
      return this.model.view = this;
    };
    ColorBoxView.prototype.render = function() {
      if (this.model.first()) {
        renderTemplate.call(this);
      }
      return this;
    };
    return ColorBoxView;
  })();
  AppView = (function() {
    __extends(AppView, Backbone.View);
    function AppView() {
      AppView.__super__.constructor.apply(this, arguments);
    }
    AppView.prototype.el = $('#config-app');
    AppView.prototype.initialize = function() {
      var color_input, view, x, _results;
      this.model = new ConfigCollection;
      color_input = new ConfigInputView({
        'el': $('#config-input'),
        'model': this.model
      });
      _results = [];
      for (x = 1; x <= 3; x++) {
        view = new ColorBoxView({
          "model": this.model
        });
        _results.push($('#color-boxes').append(view.render().el));
      }
      return _results;
    };
    return AppView;
  })();
  $(document).ready(function() {
    window.App = new AppView;
    try {
      window.App.model.reset($.parseJSON($('#configmodel-bootstrap').html()));
    } catch (error) {
      console.log("No bootstrapping found");
    }
    if (!window.App.model.first()) {
      window.App.model.add(new ConfigModel);
      return window.App.model.trigger('reset');
    }
  });
}).call(this);
