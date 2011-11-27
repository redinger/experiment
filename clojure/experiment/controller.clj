(ns experiment.controller
  (:use experiment.infra.models
	experiment.models.events
	experiment.models.user)
  (:require [quartz-clj.core :as q]
	    [experiment.libs.datetime :as dt]
	    [clj-time.core :as t]))


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

;; Queue of events to handle
;; - Reminders
;; - Instrument sampling
;; - Downloads
;; - Cleanup?  Recompute user denorm stats?

(defn reminder-task
  "An external function allows us to update task functionality
   without resubmiting it"
  [context]
  (println "Running hourly task"))

(q/defjob ReminderTask [context]
  (reminder-task context))

(defn download-task
  "An external function allows us to update task functionality
   without resubmiting it"
  [context]
  (println "Running daily download task"))

(q/defjob DownloadTask [context]
  (download-task context))

;;
;; TOP LEVEL 
;;

(defn hourly-schedule []
  (q/simple-schedule :hours 1 :forever true))

(defn daily-schedule []
  (q/cron-schedule "0 30 0 * * ?"))

(defn start []
  (q/start)
  (q/schedule-task ["Reminders" "experiment"] ReminderTask
		   (hourly-schedule))
  (q/schedule-task ["Downloads" "experiment"] DownloadTask
		   (daily-schedule)))

(defn pause []
  (q/pause))
   
(defn stop []
  (q/shutdown))



;; TESTING

(q/defjob Test1 [context]
  (let [time (.getFireTime context)
	key (.getKey (.getJobDetail context))]
    (println (format "Job [%s] fired at %s" key time))))

(defn run-test [num seconds-interval]
  (q/schedule-repeated-task ["test1" "testgroup"] Test1 num seconds-interval))
