#
# JOURNAL VIEW
#

define ['models/infra', 'models/core', 'views/common', 'views/widgets', 'use!jqueryTimeAgo', 'use!Handlebars', 'use!Backbone'],
  (Infra, Core, Common, Widgets) ->

    class OldJournalView extends Backbone.View
      @implements Widgets.TemplateView
      initialize: ->
            @initTemplate '#journal-viewer'
            @model.on('change', @render) if @model
            @mtype = @options.type
            @paging = @options.paging
            @page = @options.page or 1
            @size = @options.pagesize or 1
            @editable = @options.editable or false
            @editing = false

      render: =>
            if not @model
                    @$el.html("<h3>No Journal Entries Found</h3>")
                    return @
            else
                    entries = @model.get('journal')
                    entries = _.sortBy(entries, (x) -> x.date).reverse() if entries
                    if @options.paging
                            base = (@page - 1) * @size
                            bounds = @page * @size
                            entries = _.toArray(entries).slice(base, bounds)
                    @$el.empty()

                    args =
                            page: @page
                            total: Math.ceil @model.get('journal').length / @size
                            entries: entries
                    if @mtype
                            args['type'] = @mtype
                            args['context'] = " " #@model.get('experiment').get('title')
                    @inlineTemplate args

            if @editing
                    @editView()
            else
                    @journalView()
            @$('button.prev').attr('disabled', true) if @page == 1
            @$('button.next').attr('disabled', true) if (@page * @size) >= @model.get('journal').length
            @

      finalize: ->
            @journalView()
            @delegateEvents()

      events:
            'click .create': 'edit'
            'click .submit': 'submit'
            'click .cancel': 'cancel'
            'click .next': 'nextPage'
            'click .prev': 'prevPage'

      journalView: ->
            @$('div.edit').hide()
            @$('div.create').show() if @editable
            @$('div.journal-entry').show()

      editView: ->
            @$('div.create').hide()
            @$('div.journal-entry').hide()
            @$('div.edit').show()

      nextPage: =>
            unless @editing or (@page * @size) > @model.get('journal').length - 1
                    @page = @page + 1
                    @render()

      prevPage: =>
            unless @editing or @page <= 1
                    @page = @page - 1
                    @render()

      edit: =>
            @editing = true
            @editView()
            @

      submit: =>
            @model.annotate 'journal', @$('textarea').val()
            @journalView()
            @page = 1
            @editing = false
            @

      cancel: =>
            @journalView()
            @editing = false

    # Journal Entry View
    # -------------------------------------------------

    class JournalView extends Backbone.View
      tagName: 'div'
      className: 'journal-view'
      initialize: (options = {}) ->
          options.tid ?= 'journal-view'
          @setEntry @model or null
          @template = Infra.templateLoader.getTemplate options.tid

      setEntry: (model) ->
          if not (@model is model)
             @$el.fadeOut('fast', =>
                @model = model
                @undelegateEvents()
                @render()
                @delegateEvents()
                @$el.fadeIn('fast'))

      render: =>
          if _.isObject @model
             @
             @$el.html @template @model.toJSON()
          else
             @$el.html "<div></div>"
          @

      events:
          'keyup': 'handleKey'
          'click #sharing .option': 'changeSharing'
          'click #purpose .option': 'changePurpose'

      handleKey: (e) =>
        if not @keyChangeHandler
           @keyChangeHandler =
             _.debounce =>
               @updateContent()
             , 1000
        @keyChangeHandler()

      updateContent: () =>
        @model.save
          short: $('#journal-short').val()
          content: $('#journal-content').val()
        ,
          spinner: false

      changeButton: (target, attribute) ->
        label = $(target).html()
        @model.save attribute, label
        $(target).parents('.btn-group').
            children('a').
            contents().
            slice(0,1).
            replaceWith label + " "

      changePurpose: (e) =>
        @changeButton e.target, 'annotation'

      changeSharing: (e) =>
        @changeButton e.target, 'sharing'


    # Journal List View
    # -------------------------------------------------

    class JournalList extends Backbone.View
      tagName: 'div'
      className: 'journal-list'
      initialize: (options = {}) ->
          options.tid ?= 'journal-list'
          @template = Infra.templateLoader.getTemplate options.tid
          @collection.on 'add remove', @render
          @collection.on 'change', @maybeRender

      maybeRender: (model) =>
          if not model.hasChanged('content')
             @render()

      render: =>
          @$el.html @template
              journals: @collection.map (model) ->
                  model.toJSON()
          @

      events:
          'click .new': 'newEntry'
          'click button.del': 'delEntry'
          'click tr': 'selectEntry'

      modelFromRow: (row) ->
          id = $(row).attr('data')
          @collection.find (model) ->
              model.id is id

      newEntry: (event) =>
          now = new Date()
          @collection.create
              date: now.getTime()
              sharing: "Private"
              annotation: "Note"
              content: ""
              short: ""
              type: "journal"

      delEntry: (event) =>
          model = @modelFromRow $(event.target).parents('tr')
          if not model? then throw new Error('Invalid model in delete')
          Common.modalMessage.showMessage
            header: "Confirm Deletion"
            message: "Are you certain you want to delete your journal entry '#{ model.get('short') }' dated #{ model.get('date-str') }?"
            accept: "Delete"
            reject: "Cancel"
            callback: (result) =>
              if result is 'accept'
                  @collection.remove(model)
                  model.destroy()

      selectEntry: (event) =>
          model = @modelFromRow event.currentTarget
          @trigger 'selectEntry', model


    # Journal Page View
    # -------------------------------------------------

    class JournalPage extends Backbone.View
      initialize: (options) ->
          # Setup models and subviews
          if not @collection? then throw new Error("JournalPage requires a valid journal collection")
          @collection.comparator = (date1, date2) ->
              if date1 > date2
                 1
              else if date1 is date2
                 0
              else
                 -1
          @collection.sort()

          @template = Infra.templateLoader.getTemplate options.tid or 'journal-page'
          @jview = new JournalView
            model: null
          @jlist = new JournalList
            collection: @collection

          # Bridge events between entry and list view
          @jlist.on 'selectEntry', @setview, @
          @collection.on 'add', @setview, @
          @collection.on 'remove', @resetview, @
          @collection.on 'reset', @render, @
          @

      render: ->
          @$el.html @template {}
          @$('.jvp').append @jview.render().el
          @$('.jvl').append @jlist.render().el
          @

      setview: (model) ->
          @jview.setEntry model

      resetview: (model) ->
          @jview.setEntry null


    # Return view classes
    View: JournalView
    List: JournalList
    Page: JournalPage