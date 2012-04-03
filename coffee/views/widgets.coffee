# ------------------------------------------------------------
# View Widgets
# ------------------------------------------------------------

define ['models/infra', 'use!Backbone'],
  (Infra) ->

    #
    # TemplateView - default template support for Backbone.View apps
    #
    class TemplateView
      getTemplate: (name) ->
            try
              html = $(name).html()
              alert 'No template found for #{ name }' unless name
              Handlebars.compile html
            catch error
              alert "Error loading template #{ name } ... #{ error }"

      initTemplate: (name) ->
            @template = @getTemplate name
            @

      resolveModel: (model) ->
            if not model
               model = @model
            if not model
               model = new Backbone.Model({})
            if not model.toJSON
               model = new Backbone.Model(model)
            model

      renderTemplate: (model, template) ->
            model = @resolveModel model
            template or= @template
            @$el.html template model.toJSON()
            @

      inlineTemplate: (model, template) ->
            model = @resolveModel model
            template or= @template
            @$el.append template model.toJSON()


    #
    # SwitchPane - A view that can be managed by a container, such as the switcher
    #
    class SwitchPane
      visiblep: =>
            if @$el.is(':visible')
               true
            else
               false

      hidePane: =>
            if @visiblep()
    #           @$el.fadeOut('fast')
               @$el.hide()
    #        promise = $.Deferred (dfd) => @el.fadeOut('fast', dfd.resolve)
    #        promise.promise()

      showPane: =>
            if not @visiblep()
    #           @$el.fadeIn('fast')
               @$el.show()
    #        promise = $.Deferred (dfd) => @el.fadeIn('fast', dfd.resolve)
    #        promise.promise()

      dispatch: (path) =>
            @


    #
    # PaneSwitcher - Manages a set of AppView objects, only one of which
    #    should be displayed at a time
    #
    class PaneSwitcher extends Backbone.View
      # List of panes
      panes: {}
      initialize: ->
            @panes = @options.panes
            @

      render: =>
            @$el.empty()
            @$el.append pane.render().el for name,pane of @panes
            @

      # Switch panes
      hideOtherPanes: (target) ->
            if not target
               (pane.hidePane() if pane) for name, pane of @panes
            else if typeof target == 'string'
               (pane.hidePane() if pane and name != target) for own name, pane of @panes
            else
               (pane.hidePane() if pane and pane != target) for own name, pane of @panes

      switch: (name) =>
            @active = name
            pane = @getPane name
            if pane == null
              alert 'no pane for name ' + name
            @hideOtherPanes name
            pane.showPane()

      # Modify list
      addPane: (name, pane) ->
            @panes[name] = pane
            @trigger('switcher:add')
            @render()
            @

      removePane: (ref) ->
            if typeof ref == 'string'
               delete @panes[ref]
            else
               name = _.detect @panes, (name, pane) ->
                    if (pane == ref)
                            name
               delete @panes[name]
            @trigger('switcher:remove')
            @render()
            @

      getPane: (name) ->
            pane = @panes[name]
            if typeof pane == 'undefined'
               alert 'Pane not found for name ' + name
            pane

    TemplateView: TemplateView
    SwitchPane: SwitchPane
    PaneSwitcher: PaneSwitcher
