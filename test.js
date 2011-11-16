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
    $([trigger.get(0), popup.get(0)]).mouseover(__bind(function() {
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
	    display: 'block'.animate({
		bottom: "+=" + distance + "px",
		opacity: 1
	    }, time, 'swing')
	}, __bind(function() {
	    beingShown = false;
	    return shown = true;
	}, this));
    }, this)).mouseout(__bind(function() {
	if (hideDelayTimer) {
	    clearTimeout(hideDelayTimer);
	}
	popup.animate({
	    bottom: "-=" + distance + "px",
	    opacity: 0
	}, time, 'swing', __bind(function() {
	    shown = false;
	    popup.css('display', 'none');
	}, this));
    }, this));
}; 