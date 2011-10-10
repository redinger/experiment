(function() {
  var AppRouter, AppView, BrowserApp, BrowserFilter, BrowserFilterView, BrowserModel, Comment, HomeApp, MainMenu, Treatment, User;
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
  }, __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };
  User = (function() {
    __extends(User, Backbone.Model);
    function User() {
      User.__super__.constructor.apply(this, arguments);
    }
    User.prototype.username = function() {
      return this.get('username');
    };
    User.prototype.adminp = function() {
      return __indexOf.call(this.get('permissions'), 'admin') >= 0;
    };
    return User;
  })();
  Comment = (function() {
    __extends(Comment, Backbone.Model);
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
  Treatment = (function() {
    __extends(Treatment, Backbone.Model);
    function Treatment() {
      this.comment = __bind(this.comment, this);
      this.tag = __bind(this.tag, this);
      Treatment.__super__.constructor.apply(this, arguments);
    }
    Treatment.prototype.defaults = {
      'tags': [],
      'comments': []
    };
    Treatment.prototype.tag = function(tag) {
      return this.set({
        'tags': this.get('tags').push(tag)
      });
    };
    Treatment.prototype.comment = function(comment) {
      return this.set({
        'comments': this.get('comments').push(comment)
      });
    };
    Treatment.prototype.url = "/api/bone/treatment";
    return Treatment;
  })();
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
  BrowserFilterView = (function() {
    __extends(BrowserFilterView, Backbone.View);
    function BrowserFilterView() {
      BrowserFilterView.__super__.constructor.apply(this, arguments);
    }
    BrowserFilterView.prototype.initialize = function() {};
    return BrowserFilterView;
  })();
  AppView = (function() {
    __extends(AppView, Backbone.View);
    function AppView() {
      this.dispatch = __bind(this.dispatch, this);
      this.show = __bind(this.show, this);
      this.hide = __bind(this.hide, this);
      this.visiblep = __bind(this.visiblep, this);
      AppView.__super__.constructor.apply(this, arguments);
    }
    AppView.prototype.visiblep = function() {
      if (this.el.is(':visible')) {
        return true;
      } else {
        return false;
      }
    };
    AppView.prototype.hide = function() {
      var promise;
      if (!this.visiblep()) {
        return null;
      }
      promise = $.Deferred(__bind(function(dfd) {
        return this.el.fadeOut('fast', dfd.resolve);
      }, this));
      return promise.promise();
    };
    AppView.prototype.show = function() {
      var promise;
      if (this.visiblep()) {
        return null;
      }
      promise = $.Deferred(__bind(function(dfd) {
        return this.el.fadeIn('fast', dfd.resolve);
      }, this));
      return promise.promise();
    };
    AppView.prototype.dispatch = function(path) {
      return this.render();
    };
    return AppView;
  })();
  HomeApp = (function() {
    __extends(HomeApp, AppView);
    function HomeApp() {
      HomeApp.__super__.constructor.apply(this, arguments);
    }
    HomeApp.prototype.initialize = function() {
      this.el = $('#homeApp');
      return this;
    };
    HomeApp.prototype.render = function() {
      $(this.el).append('<h1>Hello Home Page</h1>');
      return this;
    };
    return HomeApp;
  })();
  BrowserApp = (function() {
    __extends(BrowserApp, AppView);
    function BrowserApp() {
      this.render = __bind(this.render, this);
      BrowserApp.__super__.constructor.apply(this, arguments);
    }
    BrowserApp.prototype.initialize = function() {
      this.el = $('#browserApp');
      this.filter = new BrowserFilter;
      this.mode = 'summary';
      return this;
    };
    BrowserApp.prototype.render = function() {
      $(this.el).append('<h1>Hello Browser</h1>');
      if (!$('#filterView')) {
        $(this.el).append(this.filterView.render());
      }
      if (!$('#browser')) {
        $(this.el).append(this.browser.render());
      }
      return this;
    };
    return BrowserApp;
  })();
  MainMenu = (function() {
    __extends(MainMenu, Backbone.View);
    function MainMenu() {
      this.activate = __bind(this.activate, this);
      MainMenu.__super__.constructor.apply(this, arguments);
    }
    MainMenu.prototype.menuids = ["main-menu"];
    MainMenu.prototype.initialize = function(id) {
      this.el = $(id);
      return ddsmoothmenu.init({
        'mainmenuid': 'main-menu',
        'orientation': 'v',
        'classname': 'main-menu',
        'contentsource': 'markup'
      });
    };
    MainMenu.prototype.events = {
      'click a': 'activate'
    };
    MainMenu.prototype.activate = function(event) {
      alert('got click');
      AppRouter.navigate("/app/" + item.html());
      return this;
    };
    return MainMenu;
  })();
  AppRouter = (function() {
    __extends(AppRouter, Backbone.Router);
    function AppRouter() {
      this.hideAll = __bind(this.hideAll, this);
      AppRouter.__super__.constructor.apply(this, arguments);
    }
    AppRouter.prototype.panes = [];
    AppRouter.prototype.hideAll = function() {
      var pane, _i, _len, _ref, _results;
      _ref = this.panes;
      _results = [];
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        pane = _ref[_i];
        _results.push(pane.hide());
      }
      return _results;
    };
    AppRouter.prototype.routes = {
      'app/:app/*path': 'activate',
      'app/:app': 'activate'
    };
    AppRouter.prototype.activate = function(app, path) {
      console.log('activating ' + app);
      app = (function() {
        switch (app) {
          case 'home':
            if (!this.home) {
              return this.home = new HomeApp;
            }
            break;
          case 'browse':
            if (!this.browser) {
              return this.browser = new BrowserApp;
            }
            break;
          case 'admin':
            if (!this.admin) {
              return this.admin = new AdminApp;
            }
        }
      }).call(this);
      if (app) {
        console.log('dispatching ' + path);
        this.panes.push(app);
        app.dispatch(path);
        if (!app.visiblep()) {
          this.hideAll();
          return app.show();
        }
      } else {
        this.hideAll();
        return $('#errorApp').show();
      }
    };
    return AppRouter;
  })();
  $(document).ready(function() {
    window.AppRouter = new AppRouter('#app-pane');
    window.MainMenu = new MainMenu('#main-menu');
    return Backbone.history.start({
      pushState: true
    });
  });
}).call(this);
