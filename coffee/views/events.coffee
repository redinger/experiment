####################################################
# Event Views
####################################################

define ['models/infra']
  (Infra) ->

    ## Events and Calendar

    calendarBehavior = () ->
      distance = 10
      time = 250
      hideDelay = 500
      hideDelayTimer = null
      beingShown = false
      shown = false
      trigger = $(this)
      popup = $('.events ul', this).css('opacity', 0)

      $([trigger.get(0), popup.get(0)]).mouseover () ->
            if hideDelayTimer
                    clearTimeout(hideDelayTimer)
            if beingShown or shown
                    return
            else
                    beingShown = true
            popup.css
                    bottom: 20
                    left: -76
                    display: 'block'
            .animate {bottom: "+=#{ distance }px", opacity: 1}, time, 'swing', ->
                      beingShown = false
                      shown = true
       .mouseout () ->
               clearTimeout hideDelayTimer if hideDelayTimer
               popup.animate {bottom: "-=#{ distance }px", opacity: 0},time,'swing', ->
                       shown = false
                       popup.css 'display', 'none'

    initCalendar = (url, id, month) ->
            $.get(url + id, {month: month}, (cal) =>
                    $(id).html cal
                    $('.date_has_event').each calendarBehavior)

