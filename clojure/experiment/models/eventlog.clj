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
   [experiment.models.schedule :as schedule]
   [experiment.models.trackers :as trackers]
   [experiment.models.trial :as trial]
   [experiment.models.instruments :as inst]
   ))


;; Event Timeline
;; -----------------------------------

;; ## Events

(defn event-timeline [user start end]
  (let [existing (events/get-events :user user :start start :end end)
        last-dt (:start (last existing))]
    (if (time/after? end last-dt)
      (let [interval (if (time/before? start last-dt)
                       (time/interval last-dt end)
                       (time/interval start end))]
        (concat existing
                (events/remove-scheduled
                 (trackers/all-tracker-events user interval))
                (trial/all-reminder-events user interval)))
      existing)))

;; Utilities

(defn sort-events [[date events]]
  [date (sort-by (comp dt/as-utc :start) events)])

(defn group-by-start-day [events]
  (->> events
       (group-by (fn [event]
                   (schedule/decimate
                    :day
                    (dt/in-session-tz
                     (:start event)))))
       (map sort-events)))

(defn group-events-by-day [events]
  (->> events
       group-by-start-day
       (sort-by :first)))

;; REST API For Events
;; -------------------------------------
;; Given session user, start and end
;; returns past events and future events for the period
;; 
;; Returns: [ {events: [], date: "", period: ""}, ... ]
;; 

(defn make-event-group [[start events]]
  {:date (dt/as-iso start)
   :events (vec (map #(server->client % true) events))})

;; TODO: User's trackers and trials -> track events & reminders
;; (for future time horizons)
(defapi fetch-events [:get "/api/events/fetch"]
  {:keys [start end] :as options}
  (let [start (or (dt/from-iso start) (time/minus (dt/now) (time/days 7)))
        end (or (time/plus (dt/from-iso end) (time/days 1))
                (time/plus (dt/now) (time/days 6)))
        min (time/minus (dt/now) (time/days 90))
        max (time/plus (dt/now) (time/days 90))
        start (if (time/before? start min) min start)
        end (if (time/after? end max) max end)
        user (session/current-user)
        result     (vec
                    (map make-event-group 
                         (group-events-by-day
                          (event-timeline user start end))))]
    result))

(defapi submit-event [:post "/api/events/submit"]
  {:keys [userid instid date text] :as options}
  (log/spy options)
  (let [user (resolve-dbref :user (deserialize-id userid))
        inst (resolve-dbref :instrument (deserialize-id instid))
        dt (dt/with-server-timezone
             (dt/from-iso date))
        _ (log/spy [(:_id  user) (:_id inst) dt])
        events (log/spy (events/get-events :user user :instrument inst :start dt :end dt))]
    (if (not (empty? events))
      (server->client
       (trackers/associate-message-with-events user events (dt/now) text))
      nil)))
          
