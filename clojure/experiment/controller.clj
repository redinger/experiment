(ns experiment.controller
  (:use
   experiment.infra.models
   experiment.models.user
   experiment.models.events
   experiment.models.schedule)
  (:require
   [quartz-clj.core :as q]
   [experiment.libs.datetime :as dt]
   [experiment.libs.properties :as prop]
   [clojure.tools.logging :as log]
   [clj-time.core :as time]))


;; ------------------------------
;; Scheduling Events
;; ------------------------------
;;
;; This top level event controller is a wrapper around
;; quartz-clj and the Quartz 2.1 library.  It provides support
;; for running regular maintenance tasks, including queueing and
;; operating on user events.
;;


;;
;; ## Application Job: Event Actions
;;
;; Call schedule-event to create a quartz task based on EventActionJob
;; which will trigger at the :start time, extract the event context
;; from MongoDB and call the fire-event multi-method on it.
;;
;; Depends on:
;;
;; - quartz-clj
;; - experiment.models.events   
;; - experiment.models.schedule


(defn event-from-context
  "Extract an event associated with a task "
  [ctx]
  (if-let [id ((q/context-data ctx) "eid")]
    (if-let [event (fetch-model :event {:_id (deserialize-id id)})]
      event
      (log/error (str "Event " id " not found")))
    (log/error (str "Not event ID (eid) found in job context"))))

(q/defjob EventActionJob [context]
  (let [event (event-from-context context)]
    (fire-event event)))

(defn schedule-event
  "Schedule the firing of an event stored in the mongo DB collection :event

   - :_id - must be a valid MongoDB id in :event collection
   - :start - must contain a valid time reference (long, java, or Joda date)
  "
  [event]
  (q/schedule-task [(format "EventAction-" (:_id event))
                    "experiment-events"]
                   EventActionJob
                   :start (dt/as-date (:start event))
                   :datamap {"eid" (serialize-id (:_id event))}))


;;
;; ## Site Job: Event Manager
;;
;; Every hour wake up and make sure that any pending events
;; for a few hours ahead are queued in quartz.  Keep track of
;; the end of the last interval that was queued.

(defn- next-interval
  "Maintain a horizon of 6 hours, adding 1 hour of events each time
   you are invoked (allows some margin for error)"
  [last]
  (if (nil? last)
    (time/interval (time/now) (time/plus (time/now) (time/hours 6)))
    (time/interval (.getEnd last) (time/plus (.getEnd last) (time/hours 1)))))

(defn event-manager-job
  "Loop over all event generating objects and queue any events
   within the event manager horizon defined by next-interval"
  [context]
  (when (= (prop/get :mode) :prod)
    (log/info "Running Event Scheduler task")
    (let [{:keys [last]} (q/context-data context)
          inter (next-interval last)]
      ;; for each user
      ;;    for each trial
      ;;       get events overlapping 'inter'
      ;;         schedule-event
      ;;         (logging?)  
      ;;    for each tracker
      ;;       get events overlaping 'inter'
      ;;         schedule-event
      ;;         (logging?)
      (.put context :last inter))))

(q/defjob EventJob [context]
  (event-manager-job context))

;;
;; ## Site Job: Update Manager
;;
;; 

(defn update-job
  "Update resources from 3rd party resources on a daily schedule"
  [context]
  (when (= (prop/get :mode) :prod)
    (println "Running daily download task")))

(q/defjob UpdateJob [context]
  (update-task context))


;; Top Level Controller API
;; --------------------------

(defn start []
  (q/start)
  (q/schedule-repeated-task ["Reminders" "experiment"]
                            ReminderJob
                            (q/simple-schedule :hours 1 :forever true))
  (q/schedule-repeated-task ["Updates" "experiment"]
                            UpdateJob
                            (q/cron-schedule "0 30 0 * * ?")))

(defn pause []
  (q/pause))
   
(defn stop []
  (q/shutdown))



;; ## CRONTAB Cheat Sheet
;;
;;     * * * * * command to be executed
;;     - - - - -
;;     | | | | |
;;     | | | | +- - - - day of week (0 - 6) (Sunday=0)
;;     | | | +- - - - - month (1 - 12)
;;     | | +- - - - - - day of month (1 - 31)
;;     | +- - - - - - - hour (0 - 23)
;;     +- - - - - - - - minute (0 - 59)
;;
;;     Alarm clock set to 6:30AM
;;     30 6 * * * /home/nano/alarm


;; ## TESTING

(q/defjob Test1 [context]
  (let [time (.getFireTime context)
	key (.getKey (.getJobDetail context))]

    (println (format "Job [%s] fired at %s" key time))))

(defn run-test [num seconds-interval]
  (q/schedule-repeated-task ["test1" "testgroup"] Test1 num seconds-interval))
