(ns experiment.models.trackers
  (:use experiment.infra.models
        experiment.models.user
        experiment.models.instruments)
  (:require [clj-time.core :as time]
	    [experiment.libs.datetime :as dt]
	    [experiment.models.samples :as samples]
	    [experiment.models.events :as events]))

;;
;; SMS-based Tracking
;;

(defn associate-message-with-tracker [user ts text]
  ;; TODO: Lookup trackers that can parse this?
  (:trackers user)
  false)
                                     
(defn sms-reply-handler
  "Main handler for SMS replies from our texting service.
   Given a number and message, parse it, associate it with
   an event and submit the resulting data as a sample if
   appropriate.  (TODO) Send failure messages if no parser
   matches or on a failure to parse"
  [ts number text]
  (let [ts (dt/now)
        user (user-for-cell-number number)
        
    (or (associate-message-with-events user ts text)
        (associate-message-with-tracker user ts text))))

