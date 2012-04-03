#
# JOURNAL VIEW
#

define ['models/infra', 'models/core', 'views/widgets', 'use!Handlebars', 'use!Backbone'],
  (Infra, Core, Widgets) ->

    class JournalView extends Backbone.View
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

    JournalView: JournalView
