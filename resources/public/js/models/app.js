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
    var Experiment, Experiments, Instrument, Instruments, Model, ModelCollection, Suggestion, Suggestions, Tracker, Trackers, Treatment, Treatments, Trial, Trials, UserModel, UserModels;
    Model = Common.Model;
    ModelCollection = Common.ModelCollection;
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
    return UserModels = (function() {
      __extends(UserModels, ModelCollection);
      function UserModels() {
        UserModels.__super__.constructor.apply(this, arguments);
      }
      UserModels.prototype.model = UserModel;
      return UserModels;
    })();
  });
}).call(this);
