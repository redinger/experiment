define ['jquery', 'models/infra', 'use!Handlebars', 'use!BackboneFormsBS', 'use!BackboneFormsEditors'],
  ($, Infra) ->

    if not Common
      Common = {}

#
# Modal Dialog Base
# ------------------------------

    class Common.ModalView extends Backbone.View
      initialize: (opts = {}) ->
        @schema = opts.schema if opts.schema?
        @template = Infra.templateLoader.getTemplate 'modal-dialog-template'
        @

      show: =>
        @$el.modal('show')

      hide: =>
        @$el.modal('hide')

      handleKey: (e) =>
        if e.which == 13
           e.preventDefault()
           @enterPressed() if @enterPressed?

# ModalMessage is a singleton class for programmatically calling
# a simple modal dialog

    class ModalMessage extends Common.ModalView
      attributes:
        id: 'modalDialogWrap'
        class: 'modal hide modalDialogWrap'

      footerTemplate: Handlebars.compile(
        "<div class='btn-toolbar'>
           <a class='btn btn-primary accept'>{{ accept }}</a>
           {{#if reject}}
              <a class='btn reject'>{{ reject }}</a>
           {{/if}}</div>")

      initialize: ->
        super()
        @

      render: =>
        @$el.html @template
                id: 'modalDialog'
                header: '<h2>' + @options.header + '</h2>'
                body: @options.message
                footer: @footerTemplate @options

        @delegateEvents()
        @$el.css('display', 'none')
        @

      showMessage: (data) =>
        if data
           @options.header = data.header
           @options.message = data.message
           @options.accept = data.accept or "Ok"
           @options.reject = data.reject or null
           @options.callback = data.callback or null
        @undelegateEvents()
        $('#modalDialogWrap').remove()
        $('body').append @render().el
        @show()

      events:
        'keyup': 'handleKey'
        'click .accept': 'accept'
        'click .reject': 'reject'

      enterPressed: =>
        @accept()

      accept: =>
        @hide()
        @options.callback('accept') if @options.callback

      reject: =>
        @hide()
        @options.callback('reject') if @options.callback

    Common.modalMessage = new ModalMessage({header: "", message: ""})

# Provide extra support for modal dialogs with form bodies

    # ## Modal Form
    #
    # Provides a base class for modal form based widgets.  Fires
    # 'form:accept', values, formObj and 'form:reject', formObj
    #
    # By default handles 'enter', and clicks on objects with class
    # .accept, .reject
    #
    # Subclasses can override accept(), reject() and event handling
    # Assumes superclass has set @schema prior to calling super()
    class Common.ModalForm extends Common.ModalView
      initialize: (options) ->
        super()
        if @schema or @schema = options.schema
           @makeForm @schema
           $('.templates').append @render().el
           @$el.css('display', 'none')
        else
           alert 'schema not initialized'
        @

      makeForm: (schema, data) ->
        @form = new Backbone.Form
                schema: schema
                data: data || {}
        @

      clearForm: ->
        vals = {}
        _(@schema).map( (k,v) -> vals[k] = "" if k.length > 0)
        @form.setValue vals
        @

      events:
        'keyup': 'handleKey'
        'click .accept': 'accept'
        'click .reject': 'reject'

      enterPressed: =>
        @accept()

      accept: =>
        @hide()
        @trigger 'form:accept', @form.getValue(), @form

      reject: =>
        @cancel()
        @trigger 'form:reject', @form

      cancel: =>
        @clearForm()
        @hide()


# Query parsing for internal javascript functions
    Common.extractParams = () ->
        qs = document.location.search.split("+").join(" ")
        re = /[?&]?([^=]+)=([^&]*)/g
        params = {}
        params[decodeURIComponent tokens[1]] = decodeURIComponent tokens[2] while tokens = re.exec qs
        params

    Common.queryParams = Common.extractParams()


# Startup event handlers and actions
# -----------------------

    $(document).ready ->
        # Spinner on ajax
        $('#spinner').bind('ajaxSend', (event, jqxhr, settings) ->
                    $(this).show() if not settings.spinner? or settings.spinner is true
                ).bind("ajaxStop", ->
                    $(this).hide()
                ).bind("ajaxError", ->
                    $(this).hide()
                )

        # Various actions
        $('.show-dform').bind 'click',
                (e) ->
                        e.preventDefault()
                        targ = $(e.target)
                        targ.siblings('.comment-form').show()
                        targ.hide()

    return Common
