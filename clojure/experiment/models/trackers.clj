(ns experiment.models.trackers
  (:use experiment.infra.models)
  (:require [clj-time.core :as time]
	    [experiment.models.samples :as data]
	    [experiment.libs.datetime :as dt]
	    [experiment.libs.rescuetime :as rt]))

;;
;; Working with Trackers
;;

(defn get-series
  "Given a set of trackers, generate the filtered dataset"
  [user inst start end]
  (data/get-samples user inst start end))

;;
;; Rescuetime
;;

;;(defn update-tracker [tracker]
;;  (let [user ( (:user tracker)
;;	inst (:instrument tracker)
;;	last-update (get-last-sample user inst)




;; (defn rt-results-to-samples [results]
;;   inst datetime value)

;; (deftracker RTDailyProductivity []
;;   (update [start end]
;; 	  (rt/results-as-hours
;; 	   (rt/productivity "day"
;; 			    (dt/as-iso-8601 start)
;; 			    (dt/as-iso-8601 end)))))

;; (deftracker RTDailySocialNetworking [user id]
;;   "RescueTime"
;;   "Daily Social Network Use"
;;   "This records the daily hours you spend on social networking sites according to the rescuetime service (primarily desktop machines)."
;;   (update [tracker user start & end]
;; 	  (rt-rows-to-samples
;; 	   (rt/with-key (get-in user [:rescuetime :api-key])
;; 	     (rt/results-as-hours
;; 	      (rt/productivity "day"
;; 			       (dt/as-iso-8601 start)
;; 			       (dt/as-iso-8601 (or (first end) (dt/now))))))


;;
;; DEPRECATED: Testing
;;

(defn make-test-tracker [user instrument datapoints max-value]
  (let [now (dt/now)
	ago (time/minus now (time/days (+ 1 datapoints)))
	data (map (fn [offset]
		    [(dt/as-utc (time/plus ago (time/days offset)))
		     (rand-int max-value)])
		  (range 0 datapoints))
	start (dt/as-utc ago)]
    (create-model!
     {:start start
      :data data
      :user (as-dbref user)
      :instrument (as-dbref instrument)
      :type "tracker"})))

(defn make-test-trackers [user]
  (doall
   (map #(make-test-tracker user % 20 10)
	(fetch-models :instrument))))
