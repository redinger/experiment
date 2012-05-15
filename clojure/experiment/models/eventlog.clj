(ns experiment.models.eventlog
  (:use
   experiment.infra.models
   experiment.infra.api)
  (:require
   [clojure.tools.logging :as log]
   [clj-time.core :as time]
   [experiment.libs.datetime :as dt]
   [noir.response :as resp]
   [experiment.infra.session :as session]
   [experiment.models.user :as user]
   [experiment.models.events :as events]
   [experiment.models.trackers :as trackers]
   [experiment.models.trial :as trial]
   [experiment.models.instruments :as inst]
   ))


;; Event Timeline
;; -----------------------------------

;; ## Events

(defn event-timeline [start end]
  (println start end)
  (let [user (session/current-user)
        existing (events/get-events :user user :start start :end end)]
    (if (time/after? end (dt/now))
      (let [interval (if (time/before? start (dt/now))
                       (time/interval (dt/now) end)
                       (time/interval start end))]
        (println interval)
        (concat existing
                (trackers/all-tracker-events user interval)
                (trial/all-reminder-events user interval)))
      existing)))

;; Utilities

(defn make-event-group [[start events]]
  {:date start
   :events (vec events)})

(defn group-events-by-day [events]
  (->> events
       (group-by :start)
       (sort-by :first)
       (map make-event-group)
       vec))

;; REST API For Events
;; -------------------------------------
;; Given session user, start and end
;; returns past events and future events for the period
;; 
;; Returns: [ {events: [], date: "", period: ""}, ... ]
;; 

(defapi fetch-events [:get "/api/events/fetch"]
  {:keys [start end] :as options}
  (println start end)
  (let [start (or (dt/from-iso-8601 start) (time/minus (dt/now) (time/days 7)))
        end (or (time/plus (dt/from-iso-8601 end) (time/days 1))
                (time/plus (dt/now) (time/days 6)))
        min (time/minus (dt/now) (time/days 90))
        max (time/plus (dt/now) (time/days 90))
        start (if (time/before? start min) min start)
        end (if (time/after? end max) max end)]
    (println start end)
    (group-events-by-day
     (map server->client
          (event-timeline start end)))))

  ;; Events within or before time horizon, include sample data?
  ;; User's trackers and trials -> track events & reminders (for future time horizons)
