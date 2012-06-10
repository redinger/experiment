(ns experiment.models.trackers
  (:use experiment.infra.models
        experiment.models.user)
  (:require [clj-time.core :as time]
            [clojure.tools.logging :as log]
            [experiment.libs.datetime :as dt]
            [experiment.libs.sms :as sms]
            [experiment.models.samples :as samples]
            [experiment.models.schedule :as schedule]
            [experiment.models.events :as event]
            [experiment.models.instruments :as inst]))

;;
;; Instrument Trackers
;; ------------------------------
;;
;; Trackers are standalone specifications of an instrument and a
;; schedule for updates in the case of manual interactions like SMS
;; These are embedded in a collection.
;;


;; Trackers with schedules can use the schedule event interface to
;; generate future events over some time interval (clj-time)

(defn tracker-events [tracker interval]
  (when-let [schedule (:schedule tracker)]
    (when (:stype schedule)
      (let [refs (select-keys tracker [:user :instrument])]
        (map #(merge % refs) (schedule/events schedule interval))))))

(defn all-tracker-events [user interval]
  (mapcat #(tracker-events % interval) (trackers user)))

(defn get-tracker [user variable & [service]]
  (when-let [inst (fetch-model :instrument {:variable variable :service service})]
    (let [ref (as-dbref inst)]
      (first (filter #(= (:instrument %) ref) (trackers user))))))
    

;; Service-based Trackers
;; -------------------------------
;; Download for service-based trackers are done automatically, no
;; explicit event generation is provided at present.

(defmacro with-tracker [[tracker user inst sched] & body]
  `(let [tracker# ~tracker
         ~user (resolve-dbref (:user tracker#))
         ~inst (resolve-dbref (:instrument tracker#))
         ~sched (:schedule tracker#)]
     ~@body))

(defn date-updated [tracker]
  (with-tracker [tracker u i s]
    (inst/last-update i u)))

(defn update [tracker & [interval]]
  (with-tracker [tracker u i s]
    (dt/with-interval [interval start end]
      (inst/refresh i u start end))))

(defn time-series [tracker & [interval]]
  (with-tracker [tracker u i s]
    (dt/with-interval [interval start end]
      (inst/time-series i u start end))))

(defn reset [tracker & [yes-im-sure]]
  (with-tracker [tracker u i s]
    (inst/reset i u)))

(defn submit-data [tracker samples]
  (with-tracker [tracker u i s]
    (let [samples (if (map? samples) (vector samples) samples)]
      (assert (samples/valid-samples? samples))
      (inst/update i u samples))))

;; SMS-based Tracker Protocol
;; -------------------------------
;;
;; We allow the web to satisfy a future or past (ignored) SMS event
;; as a hack to support manual tracking via a web interface
;;

;; ## Fire an SMS Event

(defn- sms-prefix-message [event]
  (if-let [prefix (:sms-prefix event)]
    (str (:message event) " (respond by texting '" prefix " <answer>')")))

(defmethod event/fire-event :sms [event]
  (log/spy ["Sending SMS for " event])
  (let [number (get-pref (event/event-user event) :cell)
        message (sms-prefix-message event)]
    (sms/send-sms number message)
    (let [status (if (event/requires-reply? event) "active" "done")]
      (event/set-status event status))))

;; ## Complete SMS Events on SMS reply

(defn complete-event-with-sample [user sample]
  (let [{:keys [event ts v]} sample
        {:keys [instrument]} event
        inst (resolve-dbref instrument)]
    (samples/add-samples user inst (list (dissoc sample :event)))
    (event/complete event ts v)
    (fetch-model :event {:_id (:_id event)})))
    
(defn cancel-event [user sample]
  (let [{:keys [event ts]} sample]
    (event/cancel event ts)))

(defn associate-message-with-events [user events ts text]
  (let [samples (keep (partial sms/parse-sms text ts) events)]
    (cond (empty? samples)
          (do (log/info
               (str "Failed to parse response from " (:username user)
                    ": '" text "'"))
              nil)
          (= (count samples) 1)
          (complete-event-with-sample user (first samples))
          true
          (do (log/warn "Multiple matching samples for " (:username user)
                        ": '" text "' -- removing old and associating with latest")
              (doall (map (partial cancel-event user) (butlast samples)))
              (complete-event-with-sample user (last samples))))))
  
(defn associate-message-with-user [user ts text]
  (let [events (event/get-events :user user :type "sms" :status "active")]
    (associate-message-with-events user events ts text)))

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

;; Example
;; -------------------

(comment
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
                        :sms-value-type "int"}}}))

