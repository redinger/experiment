(function() {
  var __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
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
  define(['use!Backbone', 'models/infra'], function(Backbone, Common) {
    var Experiment, Experiments, Instrument, Instruments, Journal, JournalEntries, Suggestion, Suggestions, Tracker, Trackers, Treatment, Treatments, Trial, Trials, User, Users, cacheTypes, collections, models;
    Treatment = (function() {
      __extends(Treatment, Common.Model);
      function Treatment() {
        Treatment.__super__.constructor.apply(this, arguments);
      }
      Treatment.prototype.serverType = 'treatment';
      Treatment.prototype.defaults = {
        'tags': [],
        'comments': []
      };
      return Treatment;
    })();
    Treatments = (function() {
      __extends(Treatments, Common.Collection);
      function Treatments() {
        Treatments.__super__.constructor.apply(this, arguments);
      }
      Treatments.prototype.model = Treatment;
      return Treatments;
    })();
    Instrument = (function() {
      __extends(Instrument, Common.Model);
      function Instrument() {
        Instrument.__super__.constructor.apply(this, arguments);
      }
      Instrument.prototype.serverType = 'instrument';
      return Instrument;
    })();
    Instruments = (function() {
      __extends(Instruments, Common.Collection);
      function Instruments() {
        Instruments.__super__.constructor.apply(this, arguments);
      }
      Instruments.prototype.model = Instrument;
      return Instruments;
    })();
    Experiment = (function() {
      __extends(Experiment, Common.Model);
      function Experiment() {
        Experiment.__super__.constructor.apply(this, arguments);
      }
      Experiment.prototype.serverType = 'experiment';
      Experiment.prototype.embedded = {
        treatment: ['reference', 'treatment'],
        instruments: ['references', 'Instruments']
      };
      return Experiment;
    })();
    Experiments = (function() {
      __extends(Experiments, Common.Collection);
      function Experiments() {
        Experiments.__super__.constructor.apply(this, arguments);
      }
      Experiments.prototype.model = Experiment;
      return Experiments;
    })();
    Trial = (function() {
      __extends(Trial, Common.Model);
      function Trial() {
        Trial.__super__.constructor.apply(this, arguments);
      }
      Trial.prototype.serverType = 'trial';
      Trial.prototype.embedded = {
        experiment: ['reference', 'experiment']
      };
      return Trial;
    })();
    Trials = (function() {
      __extends(Trials, Common.Collection);
      function Trials() {
        Trials.__super__.constructor.apply(this, arguments);
      }
      Trials.prototype.model = Trial;
      return Trials;
    })();
    Tracker = (function() {
      __extends(Tracker, Common.Model);
      function Tracker() {
        Tracker.__super__.constructor.apply(this, arguments);
      }
      Tracker.prototype.serverType = 'tracker';
      Tracker.prototype.embedded = {
        user: ['reference', 'user'],
        instrument: ['reference', 'instrument']
      };
      return Tracker;
    })();
    Trackers = (function() {
      __extends(Trackers, Common.Collection);
      function Trackers() {
        Trackers.__super__.constructor.apply(this, arguments);
      }
      Trackers.prototype.model = Tracker;
      return Trackers;
    })();
    Journal = (function() {
      __extends(Journal, Common.Model);
      function Journal() {
        Journal.__super__.constructor.apply(this, arguments);
      }
      Journal.prototype.serverType = 'journal';
      Journal.prototype.embedded = {
        user: ['reference', 'user']
      };
      return Journal;
    })();
    JournalEntries = (function() {
      __extends(JournalEntries, Common.Collection);
      function JournalEntries() {
        JournalEntries.__super__.constructor.apply(this, arguments);
      }
      JournalEntries.prototype.model = Journal;
      return JournalEntries;
    })();
    User = (function() {
      __extends(User, Common.Model);
      function User() {
        User.__super__.constructor.apply(this, arguments);
      }
      User.prototype.serverType = 'user';
      User.prototype.embedded = {
        trials: ['submodels', 'Trials'],
        trackers: ['submodels', 'Trackers'],
        journals: ['submodel', 'Journals']
      };
      User.prototype.username = function() {
        return this.get('username');
      };
      User.prototype.adminp = function() {
        return __indexOf.call(this.get('permissions'), 'admin') >= 0;
      };
      return User;
    })();
    Users = (function() {
      __extends(Users, Common.Collection);
      function Users() {
        Users.__super__.constructor.apply(this, arguments);
      }
      Users.prototype.model = User;
      return Users;
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
    models = {
      Treatment: Treatment,
      Instrument: Instrument,
      Experiment: Experiment,
      Trial: Trial,
      Tracker: Tracker,
      Suggestion: Suggestion,
      User: User
    };
    collections = {
      Treatments: Treatments,
      Instruments: Instruments,
      Experiments: Experiments,
      Trials: Trials,
      Trackers: Trackers,
      Suggestions: Suggestions,
      Users: Users
    };
    cacheTypes = function(themap) {
      var newmap;
      newmap = {};
      _.each(themap, function(constructor, type) {
        var tag;
        tag = constructor.prototype.serverType;
        return newmap[tag] = constructor;
      });
      return newmap;
    };
    Backbone.ReferenceCache.registerTypes(cacheTypes(models));
    Backbone.ReferenceCache.registerTypes(collections);
    return _.extend({}, models, collections);
  });
}).call(this);
