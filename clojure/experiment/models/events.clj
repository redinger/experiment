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

;; ## Legacy Hooks (to be removed)
(defn trial-reminders [trial]
  (:reminders trial))

(defn future-reminder? [start reminder]
  (assert (= (type start) java.lang.Long))
  (>= (:date reminder) start))


;; Event Model
;; --------------------------
;;
;; Status:
;;
;; - pending :: schedule but not fired
;; - active :: fired but not done (e.g. sms sent)
;; - done :: event is fully satisfied
;;
;; Required keys
;; 
;; - :type "event"
;; - :etype <event type>
;; - :user (user ref associated with the event)
;; - :inst (instrument ref associated with the event)
;;
;; Other keys:
;; 
;; - :start - target start of event
;; - :wait - whether to remain active waiting for a response
;; - :result - was the event satisfied (e.g. delivered, responded, etc)
;; - :timeout - how long to wait before failing when response is needed

(def required-event-keys [:type :etype :user :start])

(defn valid-event? [event]
  (and (= (count (select-keys required-event-keys))
          (count required-event-keys))
       (#{"pending" "active" "done"} (:status event))))

(defn active? [event]
  (or (nil? (:status event))
      (#{"pending" "active"} (:status event))))

(defn requires-reply? [event]
  (:wait event))

(defn event-user [event]
  (resolve-dbref (:user event)))
  
(defn- modify-if
  ([map key value]
     (if (contains? map key)
       (assoc map key value)
       map))
  ([map key new-key value]
     (if (contains? map key)
       (dissoc (assoc map new-key value) key)
       map)))

(defn- event-query [query]
  (-> query
      (modify-if :user (as-dbref (:user query)))
      (modify-if :type :etype (:type query))))

(defn get-events 
  "Return the events for the user associated with this incoming message"
  [{:keys [user status type] :as query}]
  (fetch-models :event (event-query query) :sort {:start 1}))

(defn set-status [event status]
  (modify-model! event {:$set {:status status}}))

(defn complete [event ts data]
  (modify-model! event {:$set {:status "done"
                               :result "success"
                               :result-ts (dt/as-date ts)
                               :result-val data}}))

(defn cancel [event & [reason]]
  (modify-model! event {:$set {:status "done"
                               :result (or reason "fail")
                               :result-ts (dt/as-date (dt/now))}}))


;; Event Actions
;; ---------------------------------------

(defmulti fire-event (comp keyword :etype))

;; By default do nothing
(defmethod fire-event :default [event]
  nil)

;; Example action event that writes to a log
(defmethod fire-event :log [event]
  (println "Log event firing")
  (log/spy event))


