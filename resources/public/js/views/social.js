(function() {
  var SocialView;
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; }, __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype;
    child.prototype = new ctor;
    child.__super__ = parent.prototype;
    return child;
  };
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
}).call(this);
