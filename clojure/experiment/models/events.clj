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
;; Events
;; ==================================

(comment
  ;; Definition of study actions over time
  {:type "schedule"
   }
  
  {:type "event"
   :action ...
   :sent? completed
   :submit? true
   }

  {:type "report"
   :start_date true
   :end_date true
   :subtype true
   }
  )

;; ==================================
;; Reminders
;; ==================================

(defn trial-reminders [trial]
  (:reminders trial))

(defn future-reminder? [start reminder]
  (assert (= (type start) java.lang.Long))
  (>= (:date reminder) start))


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
;; - :start (not stored, but required to schedule)
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

;; Parse and associate replies with events, submit data

(defmulti parse-sms
  "[instrument user message-text event]
   A method that parses an SMS response for user according to
   the :sms-parse type of the instrument, the default handlers
   uses :sms-prefix to identify the response prefix that associates
   the data with the instrument which is then treated as a sample
   for that instrument (assoc {:ts <datetime msg received>}
                              (parse-sms inst user event message))"
  (fn [message event]
    (when-let [name (:sms-parser event)]
      (keyword name))))

(defn default-sms-parser-re [event]
  (re-pattern
   (str (or (:sms-prefix event) "")
        (case (:sms-value-type event)
          nil "\\s*(\\d*)"
          "string" "\\s*([^\\s]+)"
          "float" "\\s*([\\d\\.]+)"))))

(defn default-sms-parser [message event]
  (when-let [value (second (re-matches (default-sms-parser-re event) message))]
    (case (:sms-value-type event)
      nil (Integer/parseInt value)
      "string" value
      "float" (Float/parseFloat value))))
    
(defmethod parse-sms :default [message ts event]
  (when-let [val (and (:sms-prefix event)
                      (default-sms-parser message event))]
    {:ts ts :v val :raw message :event event}))


(defn update-event [{:keys [event ts v] :as sample}]
  (modify-model! event {:$set {:status "done"
                               :result "success"
                               :result-ts (dt/as-date ts)}})
  (inst/update (resolve-dbref (:inst event))
               (resolve-dbref (:user event))
               [(dissoc sample :event)]))

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
              false)
          (= (count samples) 1)
          (update-event (first samples))
          true
          (do (log/warn "Multiple matching samples for " (:username user)
                        ": '" text "' -- removing old and associating with latest")
              (update-event (last samples))
              (doall (map cancel-event (butlast samples)))
              true))))

(defn associate-message-with-tracker [user ts text]
  ;; TODO: Lookup trackers that can parse this?
  nil)
                                     
(defn sms-reply-handler
  "Main handler for SMS replies from our texting service.
   Given a number and message, parse it, associate it with
   an event and submit the resulting data as a sample if
   appropriate.  (TODO) Send failure messages if no parser
   matches or on a failure to parse"
  [ts number text]
  (let [ts (dt/now)
        user (user-for-cell-number number)]
    (or (associate-message-with-events user ts text)
        (associate-message-with-tracker user ts text))))
