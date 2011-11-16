(function() {
  var ExModel, Instrument, Treatment;
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; }, __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype;
    child.prototype = new ctor;
    child.__super__ = parent.prototype;
    return child;
  };
  ExModel = (function() {
    __extends(ExModel, Backbone.Model);
    function ExModel() {
      this.comment = __bind(this.comment, this);
      ExModel.__super__.constructor.apply(this, arguments);
    }
    ExModel.prototype.initialize = function(models, options) {};
    ExModel.prototype.comment = function(comment) {
      return this.set({
        'comments': this.get('comments').push(comment)
      });
    };
    return ExModel;
  })();
  Treatment = (function() {
    __extends(Treatment, ExModel);
    function Treatment() {
      this.tag = __bind(this.tag, this);
      Treatment.__super__.constructor.apply(this, arguments);
    }
    Treatment.prototype.tag = function(tag) {
      return this.set({
        'tags': this.get('tags').push(tag)
      });
    };
    return Treatment;
  })();
  Instrument = (function() {
    __extends(Instrument, ExModel);
    function Instrument() {
      Instrument.__super__.constructor.apply(this, arguments);
    }
    Instrument.prototype.initialize = function(attrs) {
      this.set({
        implementedp: false
      });
      this.set(attrs);
      return this;
    };
    return Instrument;
  })();
}).call(this);
