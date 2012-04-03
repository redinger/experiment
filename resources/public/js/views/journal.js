(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; }, __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype;
    child.prototype = new ctor;
    child.__super__ = parent.prototype;
    return child;
  };
  define(['models/infra', 'models/core', 'views/widgets', 'use!Handlebars', 'use!Backbone'], function(Infra, Core, Widgets) {
    var JournalView;
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
      JournalView.implements(Widgets.TemplateView);
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
    return {
      JournalView: JournalView
    };
  });
}).call(this);
