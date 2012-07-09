(ns experiment.controller
  (:use
   experiment.infra.models
   experiment.models.user
   experiment.models.trial
   experiment.models.events
   experiment.models.trackers)
  (:require
   [quartz-clj.core :as q]
   [experiment.libs.datetime :as dt]
   [experiment.libs.properties :as prop]
   [experiment.models.instruments :as instruments]
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
    (log/error (str "No event ID (eid) found in job context"))))

;;
;; ## EventActionJob
;;
;; When an event is scheduled, this job will call fire-event
;; on the event object pulled from the DB (via context reference)
;; 

(q/defjob EventActionJob [context]
  (when-let [event (event-from-context context)]
    (fire-event event)))

;;
;; ## Setup Jobs
;;


(defn report-scheduling-event [event]
  (when event
    (log/infof "Scheduling event (%s) for %s"
               (if (:instrument event)
                 (:variable (event-inst event))
                 (:title (event-exp event)))
               (:username (event-user event))))
  event)

(defn schedule-event
  "Schedule the firing of an event stored in the mongo DB collection :event

   - :_id - must be a valid MongoDB id in :event collection
   - :start - must contain a valid time reference (long, java, or Joda date)
  "
  [event]
  (when event
    (q/schedule-task [(str "EventAction-" (:_id event))
                      "experiment-events"]
                     EventActionJob
                     :start (dt/as-date (:start event))
                     :datamap {"eid" (serialize-id (:_id event))})))

(defn schedule-tracker [tracker inter]
  (doseq [event (tracker-events tracker inter)]
    (-> event
        register-event
        report-scheduling-event
        schedule-event)))

(defn schedule-reminder [trial inter]
  (doseq [event (reminder-events trial inter)]
    (-> event
        register-event
        report-scheduling-event
        schedule-event)))

;;
;; ## Site Job: Event Manager
;;
;; Every hour wake up and make sure that any pending events
;; for a few hours ahead are queued in quartz.  Keep track of
;; the end of the last interval that was queued.

(def ^{:documentation "How many hours ahead do you want the scheduler to queue tasks?"}
  scheduling-horizon 24)

(def ^{:documentation "How many hours do you want to schedule at a time?"}
  scheduling-quantum 6)

(defn- need-to-schedule?
  "We add a quantum of events to the schedule when the end of the
   last scheduled interval is not beyond the scheduling horizon"
  [last]
  (time/after? (time/plus (time/now) (time/hours scheduling-horizon))
               (.getEnd last)))

(defn- next-interval
  "On startup, schedule out to the scheduling horizon.  Whenever"
  [last]
  (if (nil? last)
    (time/interval (time/now)
                   (time/plus (time/now) (time/hours scheduling-horizon)))
    (when (need-to-schedule? last)
      (time/interval (time/now)
                     (time/plus (.getEnd last) (time/hours scheduling-quantum))))))

(def user-fields* [:trials :trackers :services :preferences :type :_id])

(defn get-expired-events []
  [])

(defn schedule-pending-events
  "Schedule any events pending within the provided interval"
  [inter]
  (doseq [user (fetch-models :user :only user-fields*)]
    (dt/with-user-timezone [user]
      (doseq [trial (trials user)]
        (instruments/safe-body
         (schedule-reminder trial inter)))
      (doseq [tracker (trackers user)]
        (instruments/safe-body
         (schedule-tracker tracker inter)))))
  (doseq [expired (get-expired-events)]
    (cancel-event expired)))


(defn event-manager-task
  "Loop over all event generating objects and queue any events
   within the event manager horizon defined by next-interval"
  [context]
  (when (= (prop/get :mode) :prod)
    (when-let [inter (next-interval (:last (q/context-data context)))]
      (log/info "Scheduling new events from " (.getStart inter) " to " (.getEnd inter))
      (schedule-pending-events inter)
      (.put context :last inter))
    context))

(q/defjob EventJob [context]
  (event-manager-task context))

;;
;; ## Site Job: Update Manager
;;
;; 

(defn update-task
  "Update resources from 3rd party resources on a daily schedule for the
   period of the last 24 hours"
  [context]
  (when (= (prop/get :mode) :prod)
    (log/info "Running daily download task")
    (let [interval (time/interval (dt/ago time/days 1) (dt/now))]
      (doseq [user (fetch-models :user)]
        (doseq [tracker (trackers user)]
          (instruments/safe-body
           (log/infof "Updating %s for %s"
                      (tracker-name tracker)
                      (:username user))
           (update tracker interval)))))))

(q/defjob UpdateJob [context]
  (update-task context))


;; Top Level Controller API
;; --------------------------

(defn start []
  (q/start)
  (q/schedule-repeated-task ["Reminders" "experiment"]
                            EventJob
                            (q/simple-schedule :hours 1 :forever true))
  (q/schedule-repeated-task ["Updates" "experiment"]
                            UpdateJob
                            (q/cron-schedule "0 30 0 * * ?")))

(defn pause []
  (q/pause))
   
(defn stop []
  (q/shutdown))



;; CRONTAB Cheat Sheet
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
