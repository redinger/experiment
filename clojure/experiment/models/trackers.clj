(ns experiment.models.trackers
  (:use experiment.infra.models
        experiment.models.user
        experiment.models.instruments)
  (:require [clj-time.core :as time]
            [clojure.tools.logging :as log]
	    [experiment.libs.datetime :as dt]
            [experiment.libs.sms :as sms]
	    [experiment.models.samples :as samples]
	    [experiment.models.events :as event]))

;;
;; Instrument Trackers
;; ------------------------------
;;
;; Trackers are associated with active trials or can be standalone
;; if a user decides to track specific parameters.  The instrument
;; template determines the parameter of the tracker.  The tracker
;; in turn dictate the specific tracking events (when appropriate)
;;
;; - :instrument
;; - :schedule

(defn tracker-summary [tracker]
  {:name tracker})

(defn tracker-summary-list [user]
  (map (comp tracker-summary embed-dbrefs)
       (trackers user)))

;; SMS-based Trackers
;; -------------------------------

;; ## Fire an SMS Event

(defmethod event/fire-event :sms [event]
  (let [number nil;;(profile-get (event-user event) :cell)
        message (:message event)]
    (sms/send-sms number message)
    (let [status (if (event/requires-reply? event) "active" "done")]
      (event/set-status status))))

;; ## Complete SMS Events on SMS reply

(defn complete-event [user sample]
  (let [{:keys [event ts v]} sample
        {:keys [inst]} event]
    (event/complete event ts v)
    (samples/add-samples user (resolve-dbref inst) (dissoc sample :event))))

(defn cancel-event [user sample]
  (let [{:keys [event ts]} sample]
    (event/cancel event ts)))

(defn associate-message-with-events [user ts text]
  (let [events (event/get-events :user user :type "sms" :status "active")
        samples (keep (partial sms/parse-sms text ts) events)]
    (cond (empty? samples)
          (do (log/info
               (str "Failed to parse response from " (:username user)
                    ": '" text "'"))
              nil)
          (= (count samples) 1)
          (complete-event user (first samples))
          true
          (do (log/warn "Multiple matching samples for " (:username user)
                        ": '" text "' -- removing old and associating with latest")
              (doall (map (partial cancel-event user) (butlast samples)))
              (complete-event user (last samples))))))

(defn associate-message-with-tracker [user ts text]
  ;; TODO: Lookup trackers that can parse unsolicited sms messages?
  (:trackers user)
  false)
                                     
(defn- user-for-cell-number [num]
  (fetch-model :user {:profile.cell num}))

(defn sms-reply-handler
  "Main handler for SMS replies from our texting service.
   Given a number and message, parse it, associate it with
   an event and submit the resulting data as a sample if
   appropriate.  (TODO) Send failure messages if no parser
   matches or on a failure to parse"
  [ts number text]
  (let [user (user-for-cell-number number)]
    (or (associate-message-with-events user ts text)
        (associate-message-with-tracker user ts text))))

;; Testing
;; -------------------

(defn make-tracker [user instrument params]
  {:type "tracker"
   :user (as-dbref user)
   :instrument (as-dbref instrument)
   :schedule {:type "schedule"
              :stype "daily"
              :times [{:hour 10 :min 0} {:hour 21 :min 0}]
              :jitter 5
              :wait true
              
              :event {:type "event"
                      :etype "sms"
                      :message "What is your energy today? Reply 'e [0-10]' where 0 is lowest and 10 is manic"
                      :sms-prefix "e"
                      :sms-value-type "int"}}})

