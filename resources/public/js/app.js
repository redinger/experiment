(function() {
  var AdminApp, AppRouter, BrowserFilter, BrowserModel, Comment, Comments, DashboardApp, Experiment, Experiments, Instrument, Instruments, MainMenu, Model, ModelCollection, ObjectView, PaneSwitcher, SearchApp, SearchFilterModel, SearchFilterView, SearchItemView, SearchListView, SearchView, SocialView, Suggestion, Suggestions, SwitchPane, TemplateView, Treatment, Treatments, Trial, TrialApp, TrialView, Trials, UserModel, calendarBehavior, findModel, implements, initCalendar, loadModels, makeModelMap, renderTrackerChart, resolveReference, searchSuggestDefaults;
  var __slice = Array.prototype.slice, __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; }, __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype;
    child.prototype = new ctor;
    child.__super__ = parent.prototype;
    return child;
  }, __indexOf = Array.prototype.indexOf || function(item) {
    for (var i = 0, l = this.length; i < l; i++) {
      if (this[i] === item) return i;
    }
    return -1;
  };
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
  findModel = function(models, id) {
    return models.find(__bind(function(mod) {
      return mod.id === id;
    }, this));
  };
  resolveReference = function(ref) {
    if (ref) {
      if (ref ? !_.isArray(ref) : void 0) {
        alert("Bad Reference [" + ref + "]");
      }
      switch (ref[0]) {
        case 'experiment':
          return findModel(window.Experiments, ref[1]);
        case 'treatment':
          return findModel(window.Treatments, ref[1]);
        case 'instrument':
          return findModel(window.Instruments, ref[1]);
        case 'trial':
          return findModel(window.MyTrials, ref[1]);
        default:
          return alert("Reference not found [" + ref + "]");
      }
    }
  };
  Model = (function() {
    __extends(Model, Backbone.Model);
    function Model() {
      this.comment = __bind(this.comment, this);
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
    Model.prototype.comment = function(text) {
      var id, type;
      id = this.get('id');
      type = this.get('type');
      $.post("/api/annotate/" + type + "/" + id + "/comment", {
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
  makeModelMap = function() {
    var exp, inst, map, treat, trial, _i, _j, _k, _l, _len, _len2, _len3, _len4, _ref, _ref2, _ref3, _ref4;
    map = {};
    _ref = window.Instruments.models;
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      inst = _ref[_i];
      map[inst.get('id')] = inst;
    }
    _ref2 = window.Experiments.models;
    for (_j = 0, _len2 = _ref2.length; _j < _len2; _j++) {
      exp = _ref2[_j];
      map[exp.get('id')] = exp;
    }
    _ref3 = window.Treatments.models;
    for (_k = 0, _len3 = _ref3.length; _k < _len3; _k++) {
      treat = _ref3[_k];
      map[treat.get('id')] = treat;
    }
    _ref4 = window.MyTrials.models;
    for (_l = 0, _len4 = _ref4.length; _l < _len4; _l++) {
      trial = _ref4[_l];
      map[trial.get('id')] = trial;
    }
    return window.modelMap = map;
  };
  window.lookupModels = function(refs) {
    var id, model, models, type, _i, _len, _ref;
    models = [];
    if (_.isArray(refs) && _.isArray(refs[0])) {
      for (_i = 0, _len = refs.length; _i < _len; _i++) {
        _ref = refs[_i], type = _ref[0], id = _ref[1];
        model = window.modelMap[id];
        if (model) {
          models.push(model);
        } else {
          alert("model not found " + id);
        }
      }
    } else if (_.isArray(refs) && _.isArray.length === 2) {
      model = window.modelMap[refs[1]];
      if (model) {
        models.push(window.modelMap[refs[1]]);
      } else {
        alert("model not found " + id);
      }
    }
    return models;
  };
  Suggestion = (function() {
    __extends(Suggestion, Backbone.Model);
    function Suggestion() {
      Suggestion.__super__.constructor.apply(this, arguments);
    }
    return Suggestion;
  })();
  Suggestions = (function() {
    __extends(Suggestions, Backbone.Collection);
    function Suggestions() {
      Suggestions.__super__.constructor.apply(this, arguments);
    }
    Suggestions.prototype.model = Suggestion;
    return Suggestions;
  })();
  window.Suggestions = new Suggestions;
  Treatment = (function() {
    __extends(Treatment, Model);
    function Treatment() {
      Treatment.__super__.constructor.apply(this, arguments);
    }
    Treatment.prototype.defaults = {
      'tags': [],
      'comments': []
    };
    return Treatment;
  })();
  Treatments = (function() {
    __extends(Treatments, ModelCollection);
    function Treatments() {
      Treatments.__super__.constructor.apply(this, arguments);
    }
    Treatments.prototype.model = Treatment;
    return Treatments;
  })();
  window.Treatments = new Treatments;
  Instrument = (function() {
    __extends(Instrument, Model);
    function Instrument() {
      Instrument.__super__.constructor.apply(this, arguments);
    }
    return Instrument;
  })();
  Instruments = (function() {
    __extends(Instruments, ModelCollection);
    function Instruments() {
      Instruments.__super__.constructor.apply(this, arguments);
    }
    Instruments.prototype.model = Instrument;
    return Instruments;
  })();
  window.Instruments = new Instruments;
  Experiment = (function() {
    __extends(Experiment, Model);
    function Experiment() {
      Experiment.__super__.constructor.apply(this, arguments);
    }
    Experiment.prototype.dbrefs = ['instruments'];
    return Experiment;
  })();
  Experiments = (function() {
    __extends(Experiments, ModelCollection);
    function Experiments() {
      Experiments.__super__.constructor.apply(this, arguments);
    }
    Experiments.prototype.model = Experiment;
    return Experiments;
  })();
  window.Experiments = new Experiments;
  Trial = (function() {
    __extends(Trial, Model);
    function Trial() {
      Trial.__super__.constructor.apply(this, arguments);
    }
    Trial.prototype.dbrefs = ['experiment'];
    return Trial;
  })();
  Trials = (function() {
    __extends(Trials, ModelCollection);
    function Trials() {
      Trials.__super__.constructor.apply(this, arguments);
    }
    Trials.prototype.model = Trial;
    return Trials;
  })();
  window.MyTrials = new Trials;
  Comment = (function() {
    __extends(Comment, Model);
    function Comment() {
      Comment.__super__.constructor.apply(this, arguments);
    }
    Comment.prototype.defaults = {
      'type': 'comment'
    };
    Comment.prototype.initialize = function(params, parent) {
      this.parent = parent;
      if (!params.type) {
        alert('No type specified');
      }
      if (this.get('responses')) {
        return this.responses = new CommentCollection(this.get('responses'));
      }
    };
    Comment.prototype.voteUp = function() {
      var votes;
      votes = this.get('votes');
      if (!votes[username]) {
        return this.set({
          'votes': votes[username] = 1
        });
      }
    };
    Comment.prototype.voteDown = function() {
      var votes;
      votes = this.get('votes')[username];
      if (!votes[username]) {
        return this.set({
          'votes': votes[username] = -1
        });
      }
    };
    Comment.prototype.addComment = function(params) {
      var response;
      response = new Comment(params.extend('parent'));
      response.save();
      return this.set('responses', this.get('responses').push(response));
    };
    return Comment;
  })();
  Comments = (function() {
    __extends(Comments, Backbone.Collection);
    function Comments() {
      Comments.__super__.constructor.apply(this, arguments);
    }
    return Comments;
  })();
  UserModel = (function() {
    __extends(UserModel, Backbone.Model);
    function UserModel() {
      UserModel.__super__.constructor.apply(this, arguments);
    }
    UserModel.prototype.username = function() {
      return this.get('username');
    };
    UserModel.prototype.adminp = function() {
      return __indexOf.call(this.get('permissions'), 'admin') >= 0;
    };
    return UserModel;
  })();
  window.User = new UserModel;
  BrowserFilter = (function() {
    __extends(BrowserFilter, Backbone.Model);
    function BrowserFilter() {
      BrowserFilter.__super__.constructor.apply(this, arguments);
    }
    BrowserFilter.prototype.defaults = {
      'type': 'all',
      'sort': ['title', 'asc']
    };
    return BrowserFilter;
  })();
  BrowserModel = (function() {
    __extends(BrowserModel, Backbone.Model);
    function BrowserModel() {
      BrowserModel.__super__.constructor.apply(this, arguments);
    }
    BrowserModel.prototype.defaults = {
      'state': 'list'
    };
    return BrowserModel;
  })();
  calendarBehavior = function() {
    var beingShown, distance, hideDelay, hideDelayTimer, popup, shown, time, trigger;
    distance = 10;
    time = 250;
    hideDelay = 500;
    hideDelayTimer = null;
    beingShown = false;
    shown = false;
    trigger = $(this);
    popup = $('.events ul', this).css('opacity', 0);
    return $([trigger.get(0), popup.get(0)]).mouseover(function() {
      if (hideDelayTimer) {
        clearTimeout(hideDelayTimer);
      }
      if (beingShown || shown) {
        return;
      } else {
        beingShown = true;
      }
      return popup.css({
        bottom: 20,
        left: -76,
        display: 'block'
      }).animate({
        bottom: "+=" + distance + "px",
        opacity: 1
      }, time, 'swing', function() {
        beingShown = false;
        return shown = true;
      });
    }).mouseout(function() {
      if (hideDelayTimer) {
        clearTimeout(hideDelayTimer);
      }
      return popup.animate({
        bottom: "-=" + distance + "px",
        opacity: 0
      }, time, 'swing', function() {
        shown = false;
        return popup.css('display', 'none');
      });
    });
  };
  initCalendar = function(url, id, month) {
    return $.get(url + id, {
      month: month
    }, __bind(function(cal) {
      $(id).html(cal);
      return $('.date_has_event').each(calendarBehavior);
    }, this));
  };
  renderTrackerChart = function(id, instrument, start, extra, options) {
    options = $(options).extend({
      chart: {
        type: 'spline',
        renderTo: id
      }
    });
    return $.getJSON("/api/charts/tracker", {
      inst: instrument.get('id'),
      start: start
    }, function(config) {
      config = $.extend(config, options);
      window.chart_config = config;
      return new Highcharts.Chart(config);
    });
  };
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
      $(this.el).html(template(model.toJSON()));
      return this;
    };
    TemplateView.prototype.inlineTemplate = function(model, template) {
      model = this.resolveModel(model);
      template || (template = this.template);
      return $(this.el).append(template(model.toJSON()));
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
      if ($(this.el).is(':visible')) {
        return true;
      } else {
        return false;
      }
    };
    SwitchPane.prototype.hidePane = function() {
      if (this.visiblep()) {
        return $(this.el).hide();
      }
    };
    SwitchPane.prototype.showPane = function() {
      if (!this.visiblep()) {
        return $(this.el).show();
      }
    };
    SwitchPane.prototype.dispatch = function(path) {
      return this.render();
    };
    return SwitchPane;
  })();
  PaneSwitcher = (function() {
    PaneSwitcher.prototype.panes = {};
    function PaneSwitcher(panes) {
      this["switch"] = __bind(this["switch"], this);      if (typeof panes === 'undefined') {
        return null;
      }
      if (typeof panes === 'object') {
        this.panes = panes;
      } else {
        alert('Unrecognized format for panes ' + panes);
      }
    }
    PaneSwitcher.prototype.add = function(name, pane) {
      return this.panes[name] = pane;
    };
    PaneSwitcher.prototype.remove = function(ref) {
      var name;
      if (typeof ref === 'string') {
        return delete this.panes[ref];
      } else {
        name = _.detect(this.panes, function(name, pane) {
          if (pane === ref) {
            return name;
          }
        });
        return delete this.panes[name];
      }
    };
    PaneSwitcher.prototype.get = function(name) {
      var pane;
      pane = this.panes[name];
      if (typeof pane === 'undefined') {
        alert('Pane not found for name ' + name);
      }
      return pane;
    };
    PaneSwitcher.prototype.hideOtherPanes = function(target) {
      var name, pane, _ref, _ref2, _ref3, _results, _results2, _results3;
      console.log(this.panes);
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
      pane = this.get(name);
      if (pane === null) {
        alert('no pane for name ' + name);
      }
      this.hideOtherPanes(name);
      return pane.showPane();
    };
    return PaneSwitcher;
  })();
  DashboardApp = (function() {
    __extends(DashboardApp, Backbone.View);
    function DashboardApp() {
      this.render = __bind(this.render, this);
      DashboardApp.__super__.constructor.apply(this, arguments);
    }
    DashboardApp.implements(TemplateView, SwitchPane);
    DashboardApp.prototype.model = window.User;
    DashboardApp.prototype.initialize = function() {
      if (this.model) {
        this.model.bind('change', this.render);
      }
      this.headerTemplate = this.getTemplate('#dashboard-header');
      return this.trialsTemplate = this.getTemplate('#trial-table');
    };
    DashboardApp.prototype.events = {};
    DashboardApp.prototype.dispatch = function() {
      if ($(this.el).empty()) {
        return this.render();
      }
    };
    DashboardApp.prototype.render = function() {
      $(this.el).append(this.headerTemplate(this.model.toJSON()));
      $(this.el).append(this.trialsTemplate({
        trials: window.MyTrials.toJSON()
      }));
      return this;
    };
    return DashboardApp;
  })();
  AdminApp = (function() {
    __extends(AdminApp, Backbone.View);
    function AdminApp() {
      this.render = __bind(this.render, this);
      AdminApp.__super__.constructor.apply(this, arguments);
    }
    AdminApp.implements(TemplateView, SwitchPane);
    AdminApp.prototype.model = window.User;
    AdminApp.prototype.initialize = function() {
      if (this.model) {
        this.model.bind('change', this.render);
      }
      return this.initTemplate('#admin-main');
    };
    AdminApp.prototype.render = function() {
      return this.renderTemplate();
    };
    return AdminApp;
  })();
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
      this.openDialog = __bind(this.openDialog, this);
      this.finalize = __bind(this.finalize, this);
      this.render = __bind(this.render, this);
      this.removeUpdate = __bind(this.removeUpdate, this);
      this.update = __bind(this.update, this);
      SearchFilterView.__super__.constructor.apply(this, arguments);
    }
    SearchFilterView.implements(TemplateView);
    SearchFilterView.prototype.initialize = function() {
      this.model = window.searchFilter || (window.searchFilter = new SearchFilterModel);
      this.initTemplate('#search-filter');
      return this.dialog = this.getTemplate('#basic-dialog');
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
      return window.Suggestions.toJSON();
    };
    SearchFilterView.prototype.render = function() {
      var message;
      $(this.el).empty();
      this.inlineTemplate();
      message = {
        title: "Search Help",
        body: "Type 'show X' to filter by experiment"
      };
      $(this.el).append(this.dialog(message));
      return this;
    };
    SearchFilterView.prototype.finalize = function() {
      var defaults;
      defaults = searchSuggestDefaults;
      defaults.selectionAdded = this.update;
      defaults.selectionRemoved = this.removeUpdate;
      $(this.el).find('#search-filter-input').autoSuggest(this.allObjects(), defaults);
      return $('input.as-values').attr('value', ',');
    };
    SearchFilterView.prototype.events = {
      'click a.help-link': 'showHelpDialog'
    };
    SearchFilterView.prototype.openDialog = function(d) {
      var h, title;
      this.container = d.container[0];
      d.overlay.show();
      d.container.show();
      $('#osx-modal-content', this.container).show();
      title = $('#osx-modal-title', this.container);
      title.show();
      h = $('#osx-modal-data', this.container).height() + title.height() + 30;
      $('#osx-container').height(h);
      $('div.close', this.container).show();
      return $('#osx-modal-data', this.container).show();
    };
    SearchFilterView.prototype.showHelpDialog = function(e) {
      e.preventDefault();
      return $('#osx-modal-content').modal({
        overlayId: 'osx-overlay',
        containerId: 'osx-container',
        position: [100],
        closeHTML: null,
        minHeight: 80,
        opacity: 40,
        overlayClose: true,
        onOpen: this.openDialog
      });
    };
    return SearchFilterView;
  })();
  SearchItemView = (function() {
    __extends(SearchItemView, Backbone.View);
    function SearchItemView() {
      this.viewModel = __bind(this.viewModel, this);
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
      $(this.el).html(this.template(this.model.toJSON()));
      return this;
    };
    SearchItemView.prototype.inspect = function() {
      return window.socialView.setContext(this.model);
    };
    SearchItemView.prototype.viewModel = function() {
      var id, type;
      window.socialView.setContext(this.model);
      type = this.model.get('type');
      id = this.model.get('id');
      return window.appRouter.navigate("/app/search/" + type + "/" + id, true);
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
      this.model.bind('change', this.updateModels);
      this.models = new Backbone.Collection;
      this.models.bind('reset', this.updateView);
      this.updateModels();
      return this;
    };
    SearchListView.prototype.allModels = function() {
      return window.Experiments.models.concat(window.Treatments.models.concat(window.Instruments.models));
    };
    SearchListView.prototype.selectModels = function(selects) {
      var models;
      selects || (selects = []);
      if (selects.length === 0) {
        models = this.allModels();
        return this.models.reset(models);
      } else {
        return $.get("/api/fsearch?query=" + selects + "&limit=" + this.limit, {}, __bind(function(refs, status, jqxhr) {
          return this.models.reset(window.lookupModels(refs));
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
      $(this.el).empty();
      _ref = this.views;
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        view = _ref[_i];
        $(this.el).append(view.render().el);
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
      return $(this.el).show();
    };
    SearchView.prototype.hide = function() {
      return $(this.el).hide();
    };
    SearchView.prototype.render = function() {
      $(this.el).append(this.filterView.el);
      $(this.el).append(this.listView.el);
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
      return $(this.el).show();
    };
    ObjectView.prototype.hide = function() {
      return $(this.el).hide();
    };
    ObjectView.prototype.setModel = function(model) {
      this.model = model;
      return this.render();
    };
    ObjectView.prototype.render = function() {
      $(this.el).empty();
      if (this.model) {
        $(this.el).append("<span class='breadcrumb'><a href='/app/search'>Search</a> -> " + this.model.attributes.type + " -> <a href='/app/search/" + this.model.attributes.type + "/" + this.model.attributes.id + "'>" + this.model.attributes.name + "</a></span>");
      }
      if (!this.model) {
        $(this.el).append("<span class='breadcrumb'><a  href='/app/search'>Search</a></span>");
      }
      if (this.model) {
        $(this.el).append(this.templateMap[this.model.get('type')](this.model.toJSON()));
      }
      return this;
    };
    ObjectView.prototype.finalize = function() {};
    return ObjectView;
  })();
  SearchApp = (function() {
    __extends(SearchApp, Backbone.View);
    function SearchApp() {
      this.render = __bind(this.render, this);
      this.dispatch = __bind(this.dispatch, this);
      this.dispatchObject = __bind(this.dispatchObject, this);
      SearchApp.__super__.constructor.apply(this, arguments);
    }
    SearchApp.implements(TemplateView, SwitchPane);
    SearchApp.prototype.initialize = function() {
      this.search = new SearchView;
      this.search.render();
      this.view = new ObjectView;
      return this;
    };
    SearchApp.prototype.dispatchObject = function(ref) {
      var models;
      models = lookupModels([ref]);
      if (models.length > 0) {
        this.view.setModel(models[0]);
        document.title = models[0].get('title');
        return true;
      } else {
        if (!models) {
          alert("no model found for " + ref);
        }
        window.appRouter.navigate("/app/search", true);
        return false;
      }
    };
    SearchApp.prototype.dispatch = function(path) {
      var ref;
      if ($(this.el).children().size() === 0) {
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
    SearchApp.prototype.render = function() {
      $(this.el).empty();
      $(this.el).append(this.search.render().el);
      $(this.el).append(this.view.render().el);
      this.search.finalize();
      this.view.finalize();
      return this;
    };
    return SearchApp;
  })();
  TrialView = (function() {
    __extends(TrialView, Backbone.View);
    function TrialView() {
      this.viewModel = __bind(this.viewModel, this);
      this.render = __bind(this.render, this);
      TrialView.__super__.constructor.apply(this, arguments);
    }
    TrialView.implements(TemplateView);
    TrialView.prototype.initialize = function(exp) {
      return this.initTemplate('#trial-list-view');
    };
    TrialView.prototype.render = function() {
      return this.renderTemplate();
    };
    TrialView.prototype.events = {
      'click': 'viewModel'
    };
    TrialView.prototype.finalize = function() {
      return this.delegateEvents();
    };
    TrialView.prototype.viewModel = function() {
      var id;
      window.socialView.setContext(model.get('experiment'));
      window.socialView.setEdit(true);
      id = this.model.get('id');
      return window.appRouter.navigate("/app/trials/" + id, true);
    };
    return TrialView;
  })();
  TrialApp = (function() {
    __extends(TrialApp, Backbone.View);
    function TrialApp() {
      this.renderList = __bind(this.renderList, this);
      this.viewModel = __bind(this.viewModel, this);
      this.render = __bind(this.render, this);
      TrialApp.__super__.constructor.apply(this, arguments);
    }
    TrialApp.implements(TemplateView, SwitchPane);
    TrialApp.prototype.newTrial = function(trial) {
      return new TrialView({
        model: trial
      });
    };
    TrialApp.prototype.initialize = function(exp) {
      this.views = window.MyTrials.map(this.newTrial);
      this.initTemplate('#trial-view-header');
      this.chartTemplate = this.getTemplate('#highchart-div');
      this.journalTemplate = this.getTemplate('#journal-entry');
      this.calendarTemplate = this.getTemplate('#small-calendar');
      this.instTableTemplate = this.getTemplate('#instrument-short-table');
      return this;
    };
    TrialApp.prototype.dispatch = function(path) {
      var model;
      if (path) {
        model = findModel(window.MyTrials, path);
        if (!model) {
          alert("no model found for " + path);
        }
        this.viewModel(model);
        this.model = model;
      } else {
        window.socialView.setEdit(false);
        this.model = null;
      }
      return this.render();
    };
    TrialApp.prototype.render = function() {
      if (this.model) {
        this.renderModel();
      } else {
        this.renderList();
      }
      return this;
    };
    TrialApp.prototype.viewModel = function(model) {
      window.socialView.setContext(model.get('experiment'));
      return window.socialView.setEdit(true);
    };
    TrialApp.prototype.renderModel = function() {
      var experiment, outcome, treatment;
      experiment = this.model.get('experiment');
      treatment = experiment.get('treatment');
      outcome = experiment.get('outcome');
      $(this.el).empty();
      this.inlineTemplate();
      this.renderCalendar(experiment);
      this.renderInstruments(experiment);
      $(this.el).append("<div class='clear'/>");
      this.renderOutcome(experiment);
      this.renderJournal();
      return this;
    };
    TrialApp.prototype.renderOutcome = function(outcome) {
      outcome = outcome.get('instruments')[0];
      $(this.el).append('<h1>Results</h1>');
      this.inlineTemplate({
        cssid: 'chart1',
        height: '150px',
        width: '500px'
      }, this.chartTemplate);
      return renderTrackerChart('chart1', outcome, 0);
    };
    TrialApp.prototype.renderCalendar = function(experiment) {
      var id, month, mydiv;
      mydiv = $("<div class='trial-calendar-wrap'/>");
      $(this.el).append(mydiv);
      mydiv.append('<h2>Schedule</h2>');
      id = experiment.get('id');
      month = "now";
      mydiv.append(this.calendarTemplate({
        id: 'trial-cal'
      }));
      return initCalendar("/api/calendar/experiment/" + id, '#trial-cal', month);
    };
    TrialApp.prototype.renderInstruments = function(experiment) {
      var data, instruments, mydiv;
      mydiv = $("<div class='trial-measures'/>");
      $(this.el).append(mydiv);
      mydiv.append('<h2>Tracking</h2>');
      instruments = experiment.get('instruments');
      data = {
        instruments: _.map(instruments, function(x) {
          return x.toJSON();
        })
      };
      return mydiv.append(this.instTableTemplate(data));
    };
    TrialApp.prototype.renderJournal = function() {
      var entry, mydiv;
      mydiv = $("<div class='trial-journal'/>");
      $(this.el).append(mydiv);
      mydiv.append('<h2>Latest Trial Journal Entry</h2>');
      entry = this.model.get('journal')[0];
      return mydiv.append(this.journalTemplate(entry));
    };
    TrialApp.prototype.renderList = function() {
      var view, _i, _j, _len, _len2, _ref, _ref2;
      $(this.el).empty();
      $(this.el).append('<h1>My Trials</h1>');
      _ref = this.views;
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        view = _ref[_i];
        $(this.el).append(view.render().el);
      }
      _ref2 = this.views;
      for (_j = 0, _len2 = _ref2.length; _j < _len2; _j++) {
        view = _ref2[_j];
        view.finalize();
      }
      return this;
    };
    return TrialApp;
  })();
  SocialView = (function() {
    __extends(SocialView, Backbone.View);
    function SocialView() {
      this.addComment = __bind(this.addComment, this);
      this.render = __bind(this.render, this);
      SocialView.__super__.constructor.apply(this, arguments);
    }
    SocialView.implements(TemplateView);
    SocialView.prototype.initialize = function(id) {
      this.el = $('#social' != null ? '#social' : id)[0];
      this.initTemplate('#comment-short-view');
      this.edit = false;
      return this.render();
    };
    SocialView.prototype.setContext = function(model) {
      this.parent = model;
      this.parent.bind('change', this.render);
      this.edit = false;
      return this.render();
    };
    SocialView.prototype.setEdit = function(flag) {
      this.edit = flag;
      return this.render();
    };
    SocialView.prototype.render = function() {
      var c, comments, _i, _len, _ref;
      $(this.el).empty();
      $(this.el).append('<h1>Discussion</h1>');
      if (this.edit) {
        $(this.el).append("<textarea rows='5' cols='20'/><button class='comment' type='button'>Comment</button>");
      }
      if (this.parent) {
        comments = this.parent.get('comments');
        if (comments) {
          _ref = _.sortBy(comments, function(x) {
            return x.date;
          }).reverse();
          for (_i = 0, _len = _ref.length; _i < _len; _i++) {
            c = _ref[_i];
            this.inlineTemplate(c);
          }
        }
      }
      this.delegateEvents();
      return this;
    };
    SocialView.prototype.events = {
      'click button.comment': 'addComment'
    };
    SocialView.prototype.addComment = function() {
      return this.parent.comment($('#social textarea').val());
    };
    return SocialView;
  })();
  AppRouter = (function() {
    __extends(AppRouter, Backbone.Router);
    function AppRouter() {
      this.activate = __bind(this.activate, this);
      AppRouter.__super__.constructor.apply(this, arguments);
    }
    AppRouter.prototype.initialize = function() {
      return this.switcher = new PaneSwitcher({
        'dashboard': new DashboardApp({
          el: $('#dashboardApp').first()
        }),
        'trials': new TrialApp({
          el: $('#trialApp').first()
        }),
        'search': new SearchApp({
          el: $('#searchApp').first()
        }),
        'admin': new AdminApp({
          el: $('#adminApp').first()
        })
      });
    };
    AppRouter.prototype.routes = {
      '/app/:app/*path': 'activate',
      '/app/:app/': 'activate',
      '/app/:app': 'activate'
    };
    AppRouter.prototype.activate = function(pname, path) {
      var pane;
      console.log('Activating ' + pname + ' with ' + path);
      if (typeof path === 'undefined') {
        window.mainMenu.setMenu(pname);
      } else {
        window.mainMenu.setMenu(pname + "/" + path);
      }
      pane = this.switcher.get(pname);
      if (pane) {
        this.switcher.hideOtherPanes();
        pane.showPane();
        pane.dispatch(path);
      } else {
        this.switcher.hideOtherPanes();
        $('#errorApp').show();
      }
      return this;
    };
    return AppRouter;
  })();
  MainMenu = (function() {
    __extends(MainMenu, Backbone.View);
    function MainMenu() {
      this.activate = __bind(this.activate, this);
      this.expand = __bind(this.expand, this);
      MainMenu.__super__.constructor.apply(this, arguments);
    }
    MainMenu.prototype.root = 'app';
    MainMenu.prototype.initialize = function() {
      var _ref;
      this.root = (_ref = this.options.root) != null ? _ref : this.root;
      this.el = $(this.options.elid).first();
      this.delegateEvents();
      return this;
    };
    MainMenu.prototype.setCurrent = function(link) {
      window.testlink = link;
      if (link) {
        this.$('a.current').removeClass('current');
        return $(link).addClass('current');
      }
    };
    MainMenu.prototype.setMenu = function(base, path) {
      var link;
      if (path) {
        link = $("a[href='/" + this.root + "/" + base + "/" + path + "']").first();
        link.parents('ul.submenu').show();
        console.log(link);
      } else {
        link = $("a[href='/" + this.root + "/" + base + "']").first();
        console.log(link);
      }
      if (link) {
        return this.setCurrent(link);
      }
    };
    MainMenu.prototype.events = {
      'click a.expand': 'expand',
      'click a.action': 'activate'
    };
    MainMenu.prototype.expand = function(event) {
      var sublist;
      event.preventDefault();
      sublist = $(event.target).next();
      return sublist.slideToggle('fast');
    };
    MainMenu.prototype.activate = function(event) {
      var newLink, target;
      event.preventDefault();
      newLink = event.target;
      this.setCurrent(newLink);
      target = $(newLink).attr('href');
      window.appRouter.navigate(target, true);
      return this;
    };
    return MainMenu;
  })();
  loadModels = function() {
    if (!window.User) {
      return window.User.fetch();
    }
  };
  $(document).ready(function() {
    var match;
    loadModels();
    window.Instruments.resolveReferences();
    window.Experiments.resolveReferences();
    window.MyTrials.resolveReferences();
    window.Treatments.resolveReferences();
    makeModelMap();
    window.socialView = new SocialView('#social');
    window.appRouter = new AppRouter;
    window.mainMenu = new MainMenu({
      elid: '#main-menu'
    });
    match = Backbone.history.start({
      pushState: true,
      root: ''
    });
    return window.appRouter.navigate(document.location.pathname, true);
  });
}).call(this);
