    # +++++++++++++++++++++
    # Social Viewer
    # +++++++++++++++++++++

    # View dispatch sends message that new social data is available
    # handler on social viewer grabs data from event and updates view
    # to point at the current 'sociable' model

    class SocialView extends Backbone.View
      @implements TemplateView
      initialize: () ->
            @initTemplate '#comment-short-view'
            @edit = false
            @render() if @model
            @

      setContext: (model) ->
            @model.off('change') if @model
            @model = model
            @model.on('change', @render)
            @edit = false
            @render()

      setEdit: (flag) ->
            @edit = flag
            @render()

      render: =>
            @$el.empty()
            @$el.append '<h1>Public Comments</h1>'
            if @edit
               @$el.append "<textarea rows='5' cols='20'/><button class='comment' type='button'>Comment</button>"
            if @parent
               comments = @parent.get 'comments'
               @inlineTemplate c for c in _.sortBy(comments, (x) -> x.date).reverse() if comments
            @delegateEvents()
            @

      # Events
      events:
            'click button.comment': 'addComment'

      addComment: =>
            @parent.annotate 'comment', @$('textarea').val()

