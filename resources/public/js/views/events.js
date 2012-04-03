(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };
  define(['models/infra'], function(Infra) {
    var calendarBehavior, initCalendar;
    calendarBehavior = function() {
      var beingShown, distance, hideDelay, hideDelayTimer, popup, shown, time, trigger;
      distance = 10;
      time = 250;
      hideDelay = 500;
      hideDelayTimer = null;
      beingShown = false;
      shown = false;
      trigger = $(this);
      popup = $('.events ul', this).css('opacity', 0);
      return $([trigger.get(0), popup.get(0)]).mouseover(function() {
        if (hideDelayTimer) {
          clearTimeout(hideDelayTimer);
        }
        if (beingShown || shown) {
          return;
        } else {
          beingShown = true;
        }
        return popup.css({
          bottom: 20,
          left: -76,
          display: 'block'
        }).animate({
          bottom: "+=" + distance + "px",
          opacity: 1
        }, time, 'swing', function() {
          beingShown = false;
          return shown = true;
        });
      }).mouseout(function() {
        if (hideDelayTimer) {
          clearTimeout(hideDelayTimer);
        }
        return popup.animate({
          bottom: "-=" + distance + "px",
          opacity: 0
        }, time, 'swing', function() {
          shown = false;
          return popup.css('display', 'none');
        });
      });
    };
    return initCalendar = function(url, id, month) {
      return $.get(url + id, {
        month: month
      }, __bind(function(cal) {
        $(id).html(cal);
        return $('.date_has_event').each(calendarBehavior);
      }, this));
    };
  });
}).call(this);
