(ns experiment.models.trackers
  (:use experiment.infra.models)
  (:require [experiment.libs.datetime :as dt]
	    [clj-time.core :as time]))

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