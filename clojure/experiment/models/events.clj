(ns experiment.models.events
  (:use experiment.infra.models)
  (:require
   [clojure.tools.logging :as log]
   [experiment.libs.datetime :as dt]
   [experiment.infra.session :as session]
   [experiment.libs.sms :as sms]
   [experiment.models.user :as user]
   [experiment.models.instruments :as inst]
   ))

;; ==================================
;; Reminders
;; ==================================

(defn trial-reminders [trial]
  (:reminders trial))

(defn future-reminder? [start reminder]
  (assert (= (type start) java.lang.Long))
  (>= (:date reminder) start))


;; ==================================
;; Events
;; ==================================

;; ==================================
;; Event API
;;
;; Status:
;; - pending :: schedule but not fired
;; - active :: fired but not done (e.g. sms sent)
;; - done :: event is fully satisfied
;;
;; Required keys
;; - :type "event"
;; - :etype <event type>
;; - :user (user associated with the event)
;;
;; Other keys:
;; - :start - target start of event
;; - :wait - whether to remain active waiting for a response
;; - :result - was the event satisfied (e.g. delivered, responded, etc)
;; - :timeout - how long to wait before failing when response is needed
;; ==================================

(def required-event-keys [:type :etype :user :start])

(defn valid-event? [event]
  (and (= (count (select-keys required-event-keys))
          (count required-event-keys))
       (#{"pending" "active" "done"} (:status event))))

(defn active-event? [event]
  (or (nil? (:status event))
      (#{"pending" "active"} (:status event))))

(defn event-requires-reply? [event]
  (:wait event))
  

;; =================================
;; Event Actions
;; =================================

(defmulti fire-event (comp keyword :etype))

;; By default do nothing


(defmethod fire-event :default [event]
  nil)

;; Simply write an event to the log

(defmethod fire-event :log [event]
  (println "Log event firing")
  (log/spy event))

;;
;; Send an SMS as instrument or reminder
;;

(defmethod fire-event :sms [event]
  (sms/send-sms nil nil)
  (let [status (if (event-requires-reply? event) "active" "done")]
    (modify-model! event
                   {:$set {:status status}})))

(defn- active-sms-events
  "Return the sms events for the user associated with this incoming message"
  ([user]
     (fetch-models :event {:user (as-dbref user) :status "active" :etype "sms"}
                   :sort {:start 1})))

(defn- user-for-cell-number [num]
  (fetch-model :user {:profile.cell num}))

(defn update-event [{:keys [event ts v] :as sample}]
  (modify-model! event {:$set {:status "done"
                               :result "success"
                               :result-ts (dt/as-date ts)
                               :value v}})
  (dissoc sample :event))
  

(defn cancel-event [event & [result]]
  (modify-model! event {:$set {:status "done"
                               :result (or result "failed")
                               :result-ts (dt/as-date (dt/now))}}))

(defn associate-message-with-events [user ts text]
  (let [events (active-sms-events user)
        samples (keep (partial parse-sms text ts) events)]
    (cond (empty? samples)
          (do (log/info
               (str "Failed to parse response from " (:username user)
                    ": '" text "'"))
              nil)
          (= (count samples) 1)
          (update-event (first samples))
          true
          (do (log/warn "Multiple matching samples for " (:username user)
                        ": '" text "' -- removing old and associating with latest")
              (doall (map cancel-event (butlast samples)))
              (update-event (last samples))))))
