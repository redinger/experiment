(function() {
  var ObjectView, SearchFilterModel, SearchFilterView, SearchItemView, SearchListView, SearchPane, SearchView, searchSuggestDefaults;
  var __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype;
    child.prototype = new ctor;
    child.__super__ = parent.prototype;
    return child;
  }, __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };
  SearchFilterModel = (function() {
    __extends(SearchFilterModel, Backbone.Model);
    function SearchFilterModel() {
      SearchFilterModel.__super__.constructor.apply(this, arguments);
    }
    return SearchFilterModel;
  })();
  searchSuggestDefaults = {
    startText: 'What are you looking for?',
    keyDelay: 200,
    resultsHighlight: false,
    neverSubmit: true,
    retrieveLimit: 20,
    selectedValuesProp: 'value',
    selectedItemProp: 'title',
    searchObjProps: 'trigger,title,search',
    resultsHighlight: false
  };
  SearchFilterView = (function() {
    __extends(SearchFilterView, Backbone.View);
    function SearchFilterView() {
      this.finalize = __bind(this.finalize, this);
      this.render = __bind(this.render, this);
      this.removeUpdate = __bind(this.removeUpdate, this);
      this.update = __bind(this.update, this);
      SearchFilterView.__super__.constructor.apply(this, arguments);
    }
    SearchFilterView.implements(TemplateView);
    SearchFilterView.prototype.initialize = function() {
      this.model = App.searchFilter || (App.searchFilter = new SearchFilterModel);
      return this.initTemplate('#search-filter');
    };
    SearchFilterView.prototype.update = function(elt) {
      var results;
      results = $('input.as-values').attr('value').split(',').filter(function(x) {
        return x.length > 1;
      });
      return this.model.set({
        filters: results
      });
    };
    SearchFilterView.prototype.removeUpdate = function(elt) {
      elt.remove();
      return this.update();
    };
    SearchFilterView.prototype.allObjects = function() {
      return App.Suggestions.toJSON();
    };
    SearchFilterView.prototype.render = function() {
      this.$el.empty();
      this.inlineTemplate();
      return this;
    };
    SearchFilterView.prototype.finalize = function() {
      var defaults;
      defaults = searchSuggestDefaults;
      defaults.selectionAdded = this.update;
      defaults.selectionRemoved = this.removeUpdate;
      this.$el.find('#search-filter-input').autoSuggest(this.allObjects(), defaults);
      return $('input.as-values').attr('value', ',');
    };
    SearchFilterView.prototype.events = {
      'click a.help-link': 'showHelpDialog'
    };
    SearchFilterView.prototype.showHelpDialog = function(e) {
      e.preventDefault();
      return root.renderDialog("Search Help", "Type 'show X' to filter by experiment");
    };
    return SearchFilterView;
  })();
  SearchItemView = (function() {
    __extends(SearchItemView, Backbone.View);
    function SearchItemView() {
      this.viewModel = __bind(this.viewModel, this);
      this.inspect = __bind(this.inspect, this);
      this.render = __bind(this.render, this);
      SearchItemView.__super__.constructor.apply(this, arguments);
    }
    SearchItemView.prototype.className = 'search-list-item';
    SearchItemView.prototype.initTemplate = function() {
      return this.template = this.parent.lookupTemplate(this.model);
    };
    SearchItemView.prototype.initialize = function() {
      this.parent = this.options.parent;
      this.initTemplate();
      return this;
    };
    SearchItemView.prototype.events = {
      'mouseover': 'inspect',
      'click': 'viewModel'
    };
    SearchItemView.prototype.render = function() {
      this.$el.html(this.template(this.model.toJSON()));
      return this;
    };
    SearchItemView.prototype.inspect = function() {
      return App.socialView.setContext(this.model);
    };
    SearchItemView.prototype.viewModel = function() {
      var id, type;
      App.socialView.setContext(this.model);
      type = this.model.get('type');
      id = this.model.get('id');
      return App.router.navigate("search/" + type + "/" + id, true);
    };
    return SearchItemView;
  })();
  SearchListView = (function() {
    __extends(SearchListView, Backbone.View);
    function SearchListView() {
      this.render = __bind(this.render, this);
      this.updateView = __bind(this.updateView, this);
      this.asItemView = __bind(this.asItemView, this);
      this.updateModels = __bind(this.updateModels, this);
      SearchListView.__super__.constructor.apply(this, arguments);
    }
    SearchListView.prototype.limit = 20;
    SearchListView.prototype.className = 'search-list';
    SearchListView.prototype.subviews = {
      experiment: '#experiment-list-view',
      treatment: '#treatment-list-view',
      instrument: '#instrument-list-view'
    };
    SearchListView.prototype.buildTemplates = function() {
      var results;
      results = {};
      _.map(this.subviews, function(id, type) {
        return results[type] = Handlebars.compile($(id).html());
      });
      return results;
    };
    SearchListView.prototype.lookupTemplate = function(model) {
      var type;
      type = model.get('type');
      return this.templates[type];
    };
    SearchListView.prototype.initialize = function() {
      this.views = [];
      this.templates = this.buildTemplates();
      this.model.on('change', this.updateModels);
      this.models = new Backbone.Collection;
      this.models.on('reset', this.updateView);
      this.updateModels();
      return this;
    };
    SearchListView.prototype.allModels = function() {
      return App.Experiments.models.concat(App.Treatments.models.concat(App.Instruments.models));
    };
    SearchListView.prototype.selectModels = function(selects) {
      var models;
      selects || (selects = []);
      if (selects.length === 0) {
        models = this.allModels();
        return this.models.reset(models);
      } else {
        return $.get("/api/fsearch?query=" + selects + "&limit=" + this.limit, {}, __bind(function(refs, status, jqxhr) {
          return this.models.reset(App.lookupModels(refs));
        }, this));
      }
    };
    SearchListView.prototype.updateModels = function() {
      var filters;
      filters = this.model.get('filters');
      return this.selectModels(filters);
    };
    SearchListView.prototype.asItemView = function(model) {
      return new SearchItemView({
        model: model,
        parent: this
      });
    };
    SearchListView.prototype.updateView = function() {
      this.views = _.map(this.models.models, this.asItemView);
      return this.render();
    };
    SearchListView.prototype.render = function() {
      var view, _i, _len, _ref;
      this.$el.empty();
      _ref = this.views;
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        view = _ref[_i];
        this.$el.append(view.render().el);
      }
      return this;
    };
    SearchListView.prototype.finalize = function() {
      var view, _i, _len, _ref, _results;
      _ref = this.views;
      _results = [];
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        view = _ref[_i];
        _results.push(view.delegateEvents());
      }
      return _results;
    };
    return SearchListView;
  })();
  SearchView = (function() {
    __extends(SearchView, Backbone.View);
    function SearchView() {
      this.finalize = __bind(this.finalize, this);
      this.render = __bind(this.render, this);
      SearchView.__super__.constructor.apply(this, arguments);
    }
    SearchView.implements(TemplateView);
    SearchView.prototype.className = "search-view";
    SearchView.prototype.initialize = function() {
      this.filterView = new SearchFilterView;
      this.filterView.render();
      this.listView = new SearchListView({
        model: this.filterView.model
      });
      return this.listView.render();
    };
    SearchView.prototype.show = function() {
      return this.$el.show();
    };
    SearchView.prototype.hide = function() {
      return this.$el.hide();
    };
    SearchView.prototype.render = function() {
      this.$el.append(this.filterView.el);
      this.$el.append(this.listView.el);
      return this;
    };
    SearchView.prototype.finalize = function() {
      this.filterView.finalize();
      return this.listView.finalize();
    };
    return SearchView;
  })();
  ObjectView = (function() {
    __extends(ObjectView, Backbone.View);
    function ObjectView() {
      this.startExperiment = __bind(this.startExperiment, this);
      this.finalize = __bind(this.finalize, this);
      this.render = __bind(this.render, this);
      this.setModel = __bind(this.setModel, this);
      ObjectView.__super__.constructor.apply(this, arguments);
    }
    ObjectView.implements(TemplateView);
    ObjectView.prototype.className = "object-view";
    ObjectView.prototype.viewMap = {
      experiment: '#experiment-view',
      treatment: '#treatment-view',
      instrument: '#instrument-view'
    };
    ObjectView.prototype.templateMap = {};
    ObjectView.prototype.initialize = function() {
      try {
        return _.map(this.viewMap, __bind(function(id, type) {
          return this.templateMap[type] = Handlebars.compile($(id).html());
        }, this));
      } catch (error) {
        return alert('Failed to load object view templates');
      }
    };
    ObjectView.prototype.show = function() {
      return this.$el.show();
    };
    ObjectView.prototype.hide = function() {
      return this.$el.hide();
    };
    ObjectView.prototype.setModel = function(model) {
      this.model = model;
      return this.render();
    };
    ObjectView.prototype.render = function() {
      this.$el.empty();
      if (this.model) {
        this.$el.append("<span class='breadcrumb'><a href='search'>Search</a> -> <a href='search'>" + this.model.attributes.type + "</a> -> <a href='search/" + this.model.attributes.type + "/" + this.model.attributes.id + "'>" + this.model.attributes.name + "</a></span>");
      }
      if (!this.model) {
        this.$el.append("<span class='breadcrumb'><a  href='search'>Search</a></span>");
      }
      if (this.model) {
        this.$el.append(this.templateMap[this.model.get('type')](this.model.toJSON()));
      }
      if (this.model) {
        App.socialView.setContext(this.model);
        App.socialView.setEdit(true);
      }
      return this;
    };
    ObjectView.prototype.finalize = function() {
      return this.delegateEvents();
    };
    ObjectView.prototype.events = {
      'click .run': 'startExperiment'
    };
    ObjectView.prototype.startExperiment = function(e) {
      e.preventDefault();
      return root.renderDialog("Configure Experiment", "Placeholder Dialog pending Forms Package");
    };
    return ObjectView;
  })();
  SearchPane = (function() {
    __extends(SearchPane, Backbone.View);
    function SearchPane() {
      this.render = __bind(this.render, this);
      this.dispatch = __bind(this.dispatch, this);
      this.dispatchObject = __bind(this.dispatchObject, this);
      SearchPane.__super__.constructor.apply(this, arguments);
    }
    SearchPane.implements(TemplateView, SwitchPane);
    SearchPane.prototype.id = 'search';
    SearchPane.prototype.initialize = function() {
      this.search = new SearchView;
      this.search.render();
      this.view = new ObjectView;
      return this;
    };
    SearchPane.prototype.dispatchObject = function(ref) {
      var models;
      models = App.lookupModels([ref]);
      if (models.length > 0) {
        this.view.setModel(models[0]);
        document.name = models[0].get('name');
        return true;
      } else {
        if (!models) {
          alert("no model found for " + ref);
        }
        App.router.navigate("search", true);
        return false;
      }
    };
    SearchPane.prototype.dispatch = function(path) {
      var ref;
      if (this.$el.children().size() === 0) {
        this.render();
      }
      if (path) {
        ref = path.split("/");
      }
      if (ref && (ref.length = 2)) {
        if (this.dispatchObject(ref)) {
          this.search.hide();
          this.view.show();
        }
      } else {
        this.search.show();
        this.view.hide();
      }
      return this;
    };
    SearchPane.prototype.render = function() {
      this.$el.empty();
      this.$el.append(this.search.render().el);
      this.$el.append(this.view.render().el);
      this.search.finalize();
      this.view.finalize();
      return this;
    };
    return SearchPane;
  })();
}).call(this);
