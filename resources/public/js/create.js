(function() {
  var CreateRouter;
  var __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype;
    child.prototype = new ctor;
    child.__super__ = parent.prototype;
    return child;
  };
  CreateRouter = (function() {
    __extends(CreateRouter, Backbone.Router);
    function CreateRouter() {
      CreateRouter.__super__.constructor.apply(this, arguments);
    }
    CreateRouter.prototype.routes = {
      'view/:type': 'review',
      'edit/:type/:id': 'edit',
      'remove/:type/:id': 'remove'
    };
    CreateRouter.prototype.review = function(type) {
      return alert('review objects');
    };
    CreateRouter.prototype.edit = function(type, id) {
      return alert('edit an object');
    };
    CreateRouter.prototype.remove = function(type, id) {
      return alert('remove an object');
    };
    return CreateRouter;
  })();
}).call(this);
