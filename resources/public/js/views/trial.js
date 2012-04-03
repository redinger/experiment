(function() {
  var TrialPane, TrialSummaryView;
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; }, __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype;
    child.prototype = new ctor;
    child.__super__ = parent.prototype;
    return child;
  };
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
}).call(this);
