# ------------------------------------------------------------
# View Widgets
# ------------------------------------------------------------

define ['models/infra', 'views/common', 'use!Backbone'],
  (Infra, Common) ->

# ## TemplateView - default template support for Backbone.View apps
#
# Mixin to support template compilation and rendering in views
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


# ## NavBar
#
# Logic that attaches to the secondary nav bar and provides support for
# one or more of the following actions in response to clicks:
#
# - Select direction (no navigation)
# - Use a router, rooted in the tab namespace, to navigate to the tab
# - Call a callback
#
    class NavBar extends Backbone.View
      initialize: (options) ->
        @select = options.select
        @callback = options.callback
        @router = options.router
        if @router
           @router.on 'route:selectTab', (tabname) ->
              @switchTo tabname
           , @
        throw new Error('NavTab has no element to attach to') if not options.el

      currentTabName: () ->
        @$('.active').children('a').attr('href')

      # Respond to navigation
      switchTo: (name) ->
        @selectTab @$("a[href='#{ name }']").parent('li')

      selectTab: (tab) ->
        # Select tab
        tab.addClass('active')
        tab.siblings().removeClass('active')
        # Hide/show targets
        name = tab.children('a').attr('href')
        target = $('#' + name)
        target.show()
        target.siblings().hide()
        tab

      # Respond to Events
      events:
        'click li': 'clickTab'

      clickTab: (event) =>
        event.preventDefault()
        tab = $(event.currentTarget)
        name = tab.children('a').attr('href')
        @callback name, tab if @callback? and name?
        @router.navigate name, {trigger: true} if @router? and name?
        @selectTab tab if @select?

    class AddTagDialog extends Common.ModalForm
      attributes:
        id: 'addTagModal'
        class: 'modal hide'

      initialize: (options) ->
        @schema =
          tags:
            title: "Tag(s)"
            editorClass: "input-xlarge"
            help: "Separate multiple short tag phrases by commas"
        super()
        @

      enterPressed: =>
        @accept()

      render: ->
        @$el.html @template
          id: 'addTagModal'
          header: '<h1>Add Tags</h1>'
          footer: "<a class='btn btn-primary accept'>Add</a>
                   <a class='btn reject'>Cancel</a>"
        @$('.modal-body').append @form.render().el
        @

# ## Related Objects -
#
# A standard view for viewing a list of objects connected to the current
# object in some way.  Views results of an ajax server call based on the
# current model

    class RelatedObjects extends Backbone.View
      attributes:
        class: "related-objects"

# ## Pagination

    class Pagination extends Backbone.View
      attributes:
        class: "pagination pagination-centered"

      initialize: (options = {}) ->
        @template = Infra.templateLoader.getTemplate 'pagination-view'
        @changePage options.page or 1, options.pages or 1
        @

      changePage: (page, total) ->
        @page = page
        if _.isString @page
          @page = parseInt @page
        @pages = total if total?
        @render()

      render: ->
        prev =
          class: if @page <= 1 then "disabled" else ""
          text: "<<"
        next =
          class: if @page < @pages then "" else "disabled"
          text: ">>"
        if @page <= 3
          nums = _.range(1,6)
        else
          nums = _.range(@page - 2, @page + 2)
        pages = _.map nums, (num) ->
          result = {text: num, class: ""}
          if num is @page
            result.class = "active"
          else if num > @pages
            result.class = "disabled"
          result
        , @
        arglist = _.flatten [prev, pages, next]
        @$el.html @template arglist
        @

      events:
        'click li': 'clickToPage'

      clickToPage: (event) =>
        type = $(event.currentTarget).attr('class')
        if not (type is 'active' or type is 'disabled')
          newpage = $(event.target).text()
          if newpage is '<<'
            @trigger 'pagination:change', @page - 1
          else if newpage is '>>'
            @trigger 'pagination:change', @page + 1
          else
            @trigger 'pagination:change', parseInt(newpage)

# ## Return the widget library
    NavBar: NavBar
    TemplateView: TemplateView
    Pagination: Pagination
    AddTagDialog: AddTagDialog
    RelatedObjects: RelatedObjects
