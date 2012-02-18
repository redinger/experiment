(function() {
  var AdminPane, App, AppRouter, Application, BrowserFilter, BrowserModel, DashboardPane, Experiment, Experiments, Instrument, Instruments, JournalView, JournalWrapper, MainMenu, Model, ModelCollection, ObjectView, PaneSwitcher, ProfilePane, SearchFilterModel, SearchFilterView, SearchItemView, SearchListView, SearchPane, SearchView, SocialView, Suggestion, Suggestions, SummaryView, SwitchPane, TemplateView, Tracker, TrackerView, Trackers, Treatment, Treatments, Trial, TrialPane, TrialSummaryView, Trials, UserModel, UserModels, calendarBehavior, findModel, implements, initCalendar, renderTrackerChart, resolveReference, returnUser, root, searchSuggestDefaults;
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
  root = typeof exports !== "undefined" && exports !== null ? exports : this;
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
  Experiment = (function() {
    __extends(Experiment, Model);
    function Experiment() {
      Experiment.__super__.constructor.apply(this, arguments);
    }
    Experiment.prototype.dbrefs = ['instruments', 'treatment'];
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
  Tracker = (function() {
    __extends(Tracker, Model);
    function Tracker() {
      Tracker.__super__.constructor.apply(this, arguments);
    }
    Tracker.prototype.dbrefs = ['user', 'instrument'];
    return Tracker;
  })();
  Trackers = (function() {
    __extends(Trackers, ModelCollection);
    function Trackers() {
      Trackers.__super__.constructor.apply(this, arguments);
    }
    Trackers.prototype.model = Tracker;
    return Trackers;
  })();
  UserModel = (function() {
    __extends(UserModel, Model);
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
  UserModels = (function() {
    __extends(UserModels, ModelCollection);
    function UserModels() {
      UserModels.__super__.constructor.apply(this, arguments);
    }
    UserModels.prototype.model = UserModel;
    return UserModels;
  })();
  Application = (function() {
    __extends(Application, Backbone.View);
    function Application() {
      this.lookupModels = __bind(this.lookupModels, this);
      this.switchApp = __bind(this.switchApp, this);
      this.render = __bind(this.render, this);
      Application.__super__.constructor.apply(this, arguments);
    }
    Application.prototype.el = $('#app-wrap');
    Application.prototype.Instruments = new Instruments;
    Application.prototype.Treatments = new Treatments;
    Application.prototype.Experiments = new Experiments;
    Application.prototype.Suggestions = new Suggestions;
    Application.prototype.MyTrials = new Trials;
    Application.prototype.MyTrackers = new Trackers;
    Application.prototype.User = new UserModel;
    Application.prototype.Users = new UserModels;
    Application.prototype.start = function() {
      if (!this.User) {
        this.User.fetch();
      }
      this.Instruments.resolveReferences();
      this.Experiments.resolveReferences();
      this.MyTrials.resolveReferences();
      this.MyTrackers.resolveReferences();
      this.Treatments.resolveReferences();
      this.makeModelMap();
      this.switcher = new PaneSwitcher({
        id: 'app-tabs',
        panes: {
          dashboard: new DashboardPane,
          search: new SearchPane,
          profile: new ProfilePane,
          admin: new AdminPane,
          trials: new TrialPane
        }
      });
      this.router = new AppRouter;
      this.router.on('route:selectApp', this.switchApp);
      this.render();
      return this;
    };
    Application.prototype.render = function() {
      this.$el.append(this.switcher.render().el);
      return this;
    };
    Application.prototype.makeModelMap = function() {
      var classes, map;
      map = {};
      classes = [this.Instruments, this.Experiments, this.Treatments, this.MyTrials, this.MyTrackers];
      _(classes).each(function(c) {
        var m, _i, _len, _ref, _results;
        _ref = c.models;
        _results = [];
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          m = _ref[_i];
          _results.push(map[m.get('id')] = m);
        }
        return _results;
      });
      return this.modelMap = map;
    };
    Application.prototype.switchApp = function(pname, path) {
      var pane;
      if (typeof path === 'undefined') {
        this.menu.setMenu(pname);
      } else {
        this.menu.setMenu(pname + "/" + path);
      }
      pane = this.switcher.getPane(pname);
      if (pane) {
        this.switcher.hideOtherPanes();
        pane.showPane();
        return pane.dispatch(path);
      } else {
        this.switcher.hideOtherPanes();
        return $('#errorApp').show();
      }
    };
    Application.prototype.lookupModels = function(refs) {
      var id, model, models, type, _i, _len, _ref;
      models = [];
      if (_.isArray(refs) && _.isArray(refs[0])) {
        for (_i = 0, _len = refs.length; _i < _len; _i++) {
          _ref = refs[_i], type = _ref[0], id = _ref[1];
          model = App.modelMap[id];
          if (model) {
            models.push(model);
          } else {
            alert("model not found " + id);
          }
        }
      } else if (_.isArray(refs) && _.isArray.length === 2) {
        model = this.modelMap[refs[1]];
        if (model) {
          models.push(this.modelMap[refs[1]]);
        } else {
          alert("model not found " + id);
        }
      }
      return models;
    };
    return Application;
  })();
  window.ExApp = new Application;
  App = window.ExApp;
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
      App.chart_config = config;
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
  JournalView = (function() {
    __extends(JournalView, Backbone.View);
    function JournalView() {
      this.cancel = __bind(this.cancel, this);
      this.submit = __bind(this.submit, this);
      this.edit = __bind(this.edit, this);
      this.prevPage = __bind(this.prevPage, this);
      this.nextPage = __bind(this.nextPage, this);
      this.render = __bind(this.render, this);
      JournalView.__super__.constructor.apply(this, arguments);
    }
    JournalView.implements(TemplateView);
    JournalView.prototype.initialize = function() {
      this.initTemplate('#journal-viewer');
      if (this.model) {
        this.model.on('change', this.render);
      }
      this.mtype = this.options.type;
      this.paging = this.options.paging;
      this.page = this.options.page || 1;
      this.size = this.options.pagesize || 1;
      this.editable = this.options.editable || false;
      return this.editing = false;
    };
    JournalView.prototype.render = function() {
      var args, base, bounds, entries;
      if (!this.model) {
        this.$el.html("<h3>No Journal Entries Found</h3>");
        return this;
      } else {
        entries = this.model.get('journal');
        if (entries) {
          entries = _.sortBy(entries, function(x) {
            return x.date;
          }).reverse();
        }
        if (this.options.paging) {
          base = (this.page - 1) * this.size;
          bounds = this.page * this.size;
          entries = _.toArray(entries).slice(base, bounds);
        }
        this.$el.empty();
        args = {
          page: this.page,
          total: Math.ceil(this.model.get('journal').length / this.size),
          entries: entries
        };
        if (this.mtype) {
          args['type'] = this.mtype;
          args['context'] = " ";
        }
        this.inlineTemplate(args);
      }
      if (this.editing) {
        this.editView();
      } else {
        this.journalView();
      }
      if (this.page === 1) {
        this.$('button.prev').attr('disabled', true);
      }
      if ((this.page * this.size) >= this.model.get('journal').length) {
        this.$('button.next').attr('disabled', true);
      }
      return this;
    };
    JournalView.prototype.finalize = function() {
      this.journalView();
      return this.delegateEvents();
    };
    JournalView.prototype.events = {
      'click .create': 'edit',
      'click .submit': 'submit',
      'click .cancel': 'cancel',
      'click .next': 'nextPage',
      'click .prev': 'prevPage'
    };
    JournalView.prototype.journalView = function() {
      this.$('div.edit').hide();
      if (this.editable) {
        this.$('div.create').show();
      }
      return this.$('div.journal-entry').show();
    };
    JournalView.prototype.editView = function() {
      this.$('div.create').hide();
      this.$('div.journal-entry').hide();
      return this.$('div.edit').show();
    };
    JournalView.prototype.nextPage = function() {
      if (!(this.editing || (this.page * this.size) > this.model.get('journal').length - 1)) {
        this.page = this.page + 1;
        return this.render();
      }
    };
    JournalView.prototype.prevPage = function() {
      if (!(this.editing || this.page <= 1)) {
        this.page = this.page - 1;
        return this.render();
      }
    };
    JournalView.prototype.edit = function() {
      this.editing = true;
      this.editView();
      return this;
    };
    JournalView.prototype.submit = function() {
      this.model.annotate('journal', this.$('textarea').val());
      this.journalView();
      this.page = 1;
      this.editing = false;
      return this;
    };
    JournalView.prototype.cancel = function() {
      this.journalView();
      return this.editing = false;
    };
    return JournalView;
  })();
  SummaryView = (function() {
    __extends(SummaryView, Backbone.View);
    function SummaryView() {
      this.render = __bind(this.render, this);
      this.newTrial = __bind(this.newTrial, this);
      SummaryView.__super__.constructor.apply(this, arguments);
    }
    SummaryView.implements(SwitchPane, TemplateView);
    SummaryView.prototype.id = 'dashboard-summary';
    SummaryView.prototype.className = 'dashboard-summary';
    SummaryView.prototype.initialize = function() {
      this.trialsTemplate = this.getTemplate('#trial-table');
      this.views = App.MyTrials.map(this.newTrial);
      App.MyTrials.on('change', this.render);
      return this.render();
    };
    SummaryView.prototype.newTrial = function(trial) {
      return new TrialSummaryView({
        model: trial
      });
    };
    SummaryView.prototype.render = function() {
      var view, _i, _j, _len, _len2, _ref, _ref2;
      this.$el.empty();
      this.$el.append('<h1>My Trials</h1>');
      if (_.isEmpty(this.views)) {
        this.$el.append("<h3>No Active Trials</h3>");
      } else {
        _ref = this.views;
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          view = _ref[_i];
          this.$el.append(view.render().el);
        }
        _ref2 = this.views;
        for (_j = 0, _len2 = _ref2.length; _j < _len2; _j++) {
          view = _ref2[_j];
          view.finalize();
        }
      }
      this.$el.append('<div class="reminders"><h2>Reminders</h2><p>&nbsp;&nbsp;[Reminders are a list of upcoming measurement/treatment events]</p></div>');
      this.$el.append('<div class="feeds"><h2>Feeds</h2><p>&nbsp;&nbsp;[Feeds are a list of comments / activity around experiments you are involved in or "watching"]</p></div>');
      return this;
    };
    SummaryView.prototype.finalize = function() {};
    return SummaryView;
  })();
  TrackerView = (function() {
    __extends(TrackerView, Backbone.View);
    function TrackerView() {
      this.render = __bind(this.render, this);
      TrackerView.__super__.constructor.apply(this, arguments);
    }
    TrackerView.implements(SwitchPane, TemplateView);
    TrackerView.prototype.className = 'dashboard-tracker';
    TrackerView.prototype.initialize = function() {
      this.initTemplate('#highchart-div');
      this.trackers = App.MyTrackers.models;
      return this;
    };
    TrackerView.prototype.render = function() {
      var tracker, _i, _len, _ref;
      this.$el.empty();
      this.$el.append('<h1>Tracking Data</h1>');
      if (_.isEmpty(this.trackers)) {
        this.$el.append('<h3>No Tracking Data Found</h3>');
      } else {
        _ref = this.trackers;
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          tracker = _ref[_i];
          this.renderChart(this.getID(tracker));
        }
      }
      return this;
    };
    TrackerView.prototype.finalize = function() {
      var cssid, inst, tracker, _i, _len, _ref, _results;
      _ref = this.trackers;
      _results = [];
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        tracker = _ref[_i];
        cssid = this.getID(tracker);
        inst = tracker.get('instrument');
        _results.push(renderTrackerChart(cssid, inst, 0));
      }
      return _results;
    };
    TrackerView.prototype.getID = function(tracker) {
      if (tracker) {
        return tracker.get('id');
      } else {
        return alert('Unrecognized tracker');
      }
    };
    TrackerView.prototype.renderChart = function(id) {
      return this.inlineTemplate({
        cssid: id,
        height: '150px',
        width: '500px'
      });
    };
    return TrackerView;
  })();
  JournalWrapper = (function() {
    __extends(JournalWrapper, Backbone.View);
    function JournalWrapper() {
      this.render = __bind(this.render, this);
      JournalWrapper.__super__.constructor.apply(this, arguments);
    }
    JournalWrapper.implements(SwitchPane);
    JournalWrapper.prototype.className = 'dashboard-journal';
    JournalWrapper.prototype.initialize = function() {
      this.view = new JournalView({
        className: 'dashboard-journal',
        model: App.MyTrials.models[0],
        paging: true,
        pagesize: 5,
        editable: false
      });
      return this;
    };
    JournalWrapper.prototype.render = function() {
      this.$el.empty();
      if (this.view) {
        this.$el.append(this.view.render().el);
      }
      return this;
    };
    JournalWrapper.prototype.finalize = function() {};
    return JournalWrapper;
  })();
  DashboardPane = (function() {
    __extends(DashboardPane, Backbone.View);
    function DashboardPane() {
      this["switch"] = __bind(this["switch"], this);
      this.render = __bind(this.render, this);
      DashboardPane.__super__.constructor.apply(this, arguments);
    }
    DashboardPane.implements(TemplateView, SwitchPane);
    DashboardPane.prototype.id = 'dashboard';
    DashboardPane.prototype.model = App.User;
    DashboardPane.prototype.initialize = function() {
      this.initTemplate('#dashboard-header');
      this.switcher = new PaneSwitcher({
        id: 'dashboard-switcher',
        panes: {
          overview: new SummaryView,
          journal: new JournalWrapper,
          tracking: new TrackerView
        }
      });
      this.switcher["switch"]('overview');
      return this;
    };
    DashboardPane.prototype.dispatch = function(path) {
      var ref;
      if (this.$el.children().size() === 0) {
        this.render();
      }
      if (path) {
        if (path) {
          ref = path.split("/");
        }
        this.$('a.tab').removeClass('active-tab');
        this.$("a[href='dashboard/" + path + "']").addClass('active-tab');
        return this.switcher["switch"](ref[0]);
      } else {
        return App.router.navigate("dashboard/" + this.switcher.active, true);
      }
    };
    DashboardPane.prototype.render = function() {
      var name, pane, _ref;
      this.renderTemplate();
      this.$el.append(this.switcher.render().el);
      _ref = this.switcher.panes;
      for (name in _ref) {
        pane = _ref[name];
        pane.finalize();
      }
      return this;
    };
    DashboardPane.prototype.events = {
      'click a.tab': 'switch'
    };
    DashboardPane.prototype["switch"] = function(e) {
      var tabpath;
      e.preventDefault();
      tabpath = $(e.target).attr('href');
      return App.router.navigate(tabpath, true);
    };
    return DashboardPane;
  })();
  AdminPane = (function() {
    __extends(AdminPane, Backbone.View);
    function AdminPane() {
      this.render = __bind(this.render, this);
      AdminPane.__super__.constructor.apply(this, arguments);
    }
    AdminPane.implements(TemplateView, SwitchPane);
    AdminPane.prototype.id = 'admin';
    AdminPane.prototype.model = App.User;
    AdminPane.prototype.initialize = function() {
      if (this.model) {
        this.model.on('change', this.render);
      }
      return this.initTemplate('#admin-main');
    };
    AdminPane.prototype.render = function() {
      return this.renderTemplate();
    };
    return AdminPane;
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
  TrialSummaryView = (function() {
    __extends(TrialSummaryView, Backbone.View);
    function TrialSummaryView() {
      this.viewModel = __bind(this.viewModel, this);
      this.render = __bind(this.render, this);
      TrialSummaryView.__super__.constructor.apply(this, arguments);
    }
    TrialSummaryView.implements(TemplateView);
    TrialSummaryView.prototype.initialize = function(exp) {
      return this.initTemplate('#trial-list-view');
    };
    TrialSummaryView.prototype.render = function() {
      return this.renderTemplate();
    };
    TrialSummaryView.prototype.events = {
      'click': 'viewModel'
    };
    TrialSummaryView.prototype.finalize = function() {
      return this.delegateEvents();
    };
    TrialSummaryView.prototype.viewModel = function() {
      var id;
      App.socialView.setContext(model.get('experiment'));
      App.socialView.setEdit(true);
      id = this.model.get('id');
      return App.router.navigate("trials/" + id, true);
    };
    return TrialSummaryView;
  })();
  TrialPane = (function() {
    __extends(TrialPane, Backbone.View);
    function TrialPane() {
      this.renderList = __bind(this.renderList, this);
      this.viewModel = __bind(this.viewModel, this);
      this.render = __bind(this.render, this);
      TrialPane.__super__.constructor.apply(this, arguments);
    }
    TrialPane.implements(TemplateView, SwitchPane);
    TrialPane.prototype.id = 'trial';
    TrialPane.prototype.newTrial = function(trial) {
      return new TrialSummaryView({
        model: trial
      });
    };
    TrialPane.prototype.initialize = function(exp) {
      this.views = App.MyTrials.map(this.newTrial);
      this.initTemplate('#trial-view-header');
      this.chartTemplate = this.getTemplate('#highchart-div');
      this.calendarTemplate = this.getTemplate('#small-calendar');
      this.instTableTemplate = this.getTemplate('#instrument-short-table');
      return this;
    };
    TrialPane.prototype.dispatch = function(path) {
      if (path) {
        this.model = findModel(App.MyTrials, path);
        if (!this.model) {
          alert("no model found for " + path);
        }
        this.journal = new JournalView({
          className: 'trial-journal',
          type: 'Trial',
          model: this.model,
          paging: true,
          editable: true,
          page: 1,
          pagesize: 1
        });
        this.viewModel(this.model);
      } else {
        App.socialView.setEdit(false);
        this.model = null;
      }
      return this.render();
    };
    TrialPane.prototype.render = function() {
      if (this.model) {
        this.renderModel();
      } else {
        this.renderList();
      }
      return this;
    };
    TrialPane.prototype.viewModel = function(model) {
      App.socialView.setContext(model.get('experiment'));
      return App.socialView.setEdit(true);
    };
    TrialPane.prototype.renderModel = function() {
      var experiment, outcome, treatment;
      experiment = this.model.get('experiment');
      treatment = experiment.get('treatment');
      outcome = experiment.get('outcome');
      this.$el.empty();
      this.inlineTemplate();
      this.renderCalendar(experiment);
      this.renderInstruments(experiment);
      this.$el.append("<div class='clear'/>");
      this.renderOutcome(experiment);
      this.$el.append(this.journal.render().el);
      this.journal.finalize();
      return this;
    };
    TrialPane.prototype.renderOutcome = function(outcome) {
      outcome = outcome.get('instruments')[0];
      this.$el.append('<h1>Results</h1>');
      this.inlineTemplate({
        cssid: 'chart1',
        height: '150px',
        width: '500px'
      }, this.chartTemplate);
      return renderTrackerChart('chart1', outcome, 0);
    };
    TrialPane.prototype.renderCalendar = function(experiment) {
      var id, month, mydiv;
      mydiv = $("<div class='trial-calendar-wrap'/>");
      this.$el.append(mydiv);
      mydiv.append('<h2>Schedule</h2>');
      id = experiment.get('id');
      month = "now";
      mydiv.append(this.calendarTemplate({
        id: 'trial-cal'
      }));
      return initCalendar("/api/calendar/experiment/" + id, '#trial-cal', month);
    };
    TrialPane.prototype.renderInstruments = function(experiment) {
      var data, instruments, mydiv;
      mydiv = $("<div class='trial-measures'/>");
      this.$el.append(mydiv);
      mydiv.append('<h2>Tracking Instruments</h2>');
      instruments = experiment.get('instruments');
      data = {
        instruments: _.map(instruments, function(x) {
          return x.toJSON();
        })
      };
      return mydiv.append(this.instTableTemplate(data));
    };
    TrialPane.prototype.renderTrials = function(trialViews) {
      var mydiv, view, _i, _j, _len, _len2, _ref, _ref2;
      mydiv = $("<div class='trial-views'/>");
      mydiv.append('<h1>My Trials</h1>');
      _ref = this.views;
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        view = _ref[_i];
        mydiv.append(view.render().el);
      }
      _ref2 = this.views;
      for (_j = 0, _len2 = _ref2.length; _j < _len2; _j++) {
        view = _ref2[_j];
        view.finalize();
      }
      this.$el.append(mydiv);
      return this;
    };
    TrialPane.prototype.renderList = function() {
      this.$el.empty();
      this.renderTrials();
      return this;
    };
    return TrialPane;
  })();
  SocialView = (function() {
    __extends(SocialView, Backbone.View);
    function SocialView() {
      this.addComment = __bind(this.addComment, this);
      this.render = __bind(this.render, this);
      SocialView.__super__.constructor.apply(this, arguments);
    }
    SocialView.implements(TemplateView);
    SocialView.prototype.initialize = function() {
      this.initTemplate('#comment-short-view');
      this.edit = false;
      if (this.model) {
        this.render();
      }
      return this;
    };
    SocialView.prototype.setContext = function(model) {
      if (this.model) {
        this.model.off('change');
      }
      this.model = model;
      this.model.on('change', this.render);
      this.edit = false;
      return this.render();
    };
    SocialView.prototype.setEdit = function(flag) {
      this.edit = flag;
      return this.render();
    };
    SocialView.prototype.render = function() {
      var c, comments, _i, _len, _ref;
      this.$el.empty();
      this.$el.append('<h1>Public Comments</h1>');
      if (this.edit) {
        this.$el.append("<textarea rows='5' cols='20'/><button class='comment' type='button'>Comment</button>");
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
      return this.parent.annotate('comment', this.$('textarea').val());
    };
    return SocialView;
  })();
  ProfilePane = (function() {
    __extends(ProfilePane, Backbone.View);
    function ProfilePane() {
      this.render = __bind(this.render, this);
      this.updateModel = __bind(this.updateModel, this);
      ProfilePane.__super__.constructor.apply(this, arguments);
    }
    ProfilePane.implements(TemplateView, SwitchPane);
    ProfilePane.prototype.id = 'profile';
    ProfilePane.prototype.model = App.User;
    ProfilePane.prototype.initialize = function() {
      return this.initTemplate('#profile-edit-view');
    };
    ProfilePane.prototype.events = {
      'change': 'updateModel'
    };
    ProfilePane.prototype.updateModel = function(e) {
      var name, target;
      e.preventDefault();
      target = $(e.target);
      name = target.prop('name');
      this.model.set(name, target.val());
      return this;
    };
    ProfilePane.prototype.render = function() {
      return this.renderTemplate(this.model.get('prefs'));
    };
    return ProfilePane;
  })();
  AppRouter = (function() {
    __extends(AppRouter, Backbone.Router);
    function AppRouter() {
      this.select = __bind(this.select, this);
      AppRouter.__super__.constructor.apply(this, arguments);
    }
    AppRouter.prototype.routes = {
      ':app/*path': 'select',
      ':app': 'select'
    };
    AppRouter.prototype.select = function(pname, path) {
      return this.trigger("route:selectApp", pname, path);
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
    MainMenu.prototype.urlBase = '/app/';
    MainMenu.prototype.initialize = function() {
      this.delegateEvents();
      return this;
    };
    MainMenu.prototype.setCurrent = function(link) {
      if (link) {
        this.$('a.current').removeClass('current');
        return $(link).addClass('current');
      }
    };
    MainMenu.prototype.setMenu = function(base, path) {
      var link;
      if (path) {
        link = $("a[href='" + this.urlBase + "/" + base + "/" + path + "']").first();
        link.parents('ul.submenu').show();
      } else {
        link = $("a[href='" + this.urlBase + "/" + base + "']").first();
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
      App.router.navigate(target, true);
      return this;
    };
    return MainMenu;
  })();
  $(document).ready(function() {
    var match;
    App.socialView = new SocialView({
      el: $('#share-pane')
    });
    App.menu = new MainMenu({
      el: $('#main-menu')
    });
    App.start();
    return match = Backbone.history.start({
      root: '/app/',
      pushState: true,
      silent: false
    });
  });
}).call(this);
