(ns experiment.controller
  (:use experiment.infra.models
	experiment.models.user
	experiment.models.events
        experiment.models.schedule)
  (:require [quartz-clj.core :as q]
	    [experiment.libs.datetime :as dt]
	    [experiment.libs.properties :as prop]
            [clojure.tools.logging :as log]
	    [clj-time.core :as t]))


;;
;; Scheduling Events
;;

;; Submit event to DB event log
;; Schedule task for target time pointing at event in DB via ID
;; On firing, 

(defn event-from-context [ctx]
  (if-let [id ((q/context-data ctx) "eid")]
    (if-let [event (fetch-model :event {:_id (deserialize-id id)})]
      event
      (log/error (str "Event " id " not found")))
    (log/error (str "Not event ID (eid) found in job context"))))

(q/defjob EventActionTask [context]
  (let [event (event-from-context context)]
    (fire-event event)))

(defn schedule-event
  "Schedule the firing of an event in the mongo DB collection :event
   :_id - must be a valid MongoDB id in :event collection
   :start - must contain a valid time reference (long, java, or Joda date)
  "
  [event]
  (q/schedule-task [(format "EventAction-" (:_id event))
                    "experiment-events"]
                   EventActionTask
                   :start (dt/as-date (:start event))
                   :data {"eid" (serialize-id (:_id event))}))


;;
;; TOP LEVEL CONTROL
;;

(defn reminder-task
  "An external function allows us to update task functionality
   without resubmiting it"
  [context]
  ;; for each user
  ;;    for each tracker
  ;;       get events
  ;;       execute events (notification/routing?)
  (when (= (prop/get :mode) :prod)
    (println "Running hourly task")))

(q/defjob ReminderTask [context]
  (reminder-task context))

(defn update-task
  "Update resources from 3rd party resources on a schedule"
  [context]
  (when (= (prop/get :mode) :prod)
    (println "Running daily download task")))

(q/defjob UpdateTask [context]
  (update-task context))

;;
;; TOP LEVEL 
;;

(defn hourly-schedule []
  (q/simple-schedule :hours 1 :forever true))

(defn daily-schedule []
  (q/cron-schedule "0 30 0 * * ?"))

(defn start []
  (q/start)
  (q/schedule-repeated-task ["Reminders" "experiment"]
                            ReminderTask
                            (hourly-schedule))
  (q/schedule-repeated-task ["Updates" "experiment"]
                            UpdateTask
                            (daily-schedule)))

(defn pause []
  (q/pause))
   
(defn stop []
  (q/shutdown))


;; TESTING

;; CRONTAB Cheat Sheet
;; ----------------------------------
;;
;; # * * * * * command to be executed
;; # - - - - -
;; # | | | | |
;; # | | | | +- - - - day of week (0 - 6) (Sunday=0)
;; # | | | +- - - - - month (1 - 12)
;; # | | +- - - - - - day of month (1 - 31)
;; # | +- - - - - - - hour (0 - 23)
;; # +- - - - - - - - minute (0 - 59)
;;
;; # Alarm clock set to 6:30AM
;; 30 6 * * * /home/nano/alarm

(q/defjob Test1 [context]
  (let [time (.getFireTime context)
	key (.getKey (.getJobDetail context))]
    (println (format "Job [%s] fired at %s" key time))))

(defn run-test [num seconds-interval]
  (q/schedule-repeated-task ["test1" "testgroup"] Test1 num seconds-interval))
