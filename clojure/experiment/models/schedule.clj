(ns experiment.models.schedule
  (:use
   experiment.infra.models
   [clj-time.core :as time :exclude [extend]])
  (:require
   [experiment.models.user :as user]
   [experiment.libs.datetime :as dt]))


;; {
;;  :type "schedule"
;;  :stype "daily"
;;  :tod <seconds>
;;  :event {...}
;; }

;;
;; Scheduler utilities
;;

(defn decimate-fn [chunksize]
  (case (keyword chunksize)
    :year #(from-time-zone (date-time (year %)) (.getZone %))
    :month #(from-time-zone (date-time (year %) (month %)) (.getZone %))
    :week #(let [date (from-time-zone (date-time (year %) (month %) (day %)) (.getZone %))]
             (.roundFloorCopy (.weekOfWeekyear date)))
    :day #(from-time-zone (date-time (year %) (month %) (day %)) (.getZone %))
    :hour #(from-time-zone (date-time (year %) (month %) (day %) (hour %)) (.getZone %))))

(defn decimate [chunksize dt]
  ((decimate-fn chunksize) dt))
                   
(defn local-now []
  (to-time-zone (now) (time-zone-for-offset -8)))

(defn random-minutes [range]
  (minutes (int (- (rand range) (/ range 2)))))

(defn jitter-event
  "Given an event with a :jitter value, apply the jitter computation
   to change the :start instant of the event to +/- (int (/ jitter 2))"
  ([event]
     (if-let [jitter (:jitter event)]
       (assoc (dissoc event :jitter)
         :start (plus (:start event) (random-minutes jitter)))
       event)))

(defn interval->days
  "Given an interval of time, return a sequence of date-time
   objects representing the starting instant of the days
   that lie within the interval in the same timezone"
  ([i]
     (lazy-seq
      (let [start (decimate :day (start i))
            newstart (plus start (days 1))]
        (if (before? newstart (end i))
          (cons start (interval->days (interval newstart (end i))))
          (cons start nil))))))

(defn interval->weeks
  "Given an interval of time, return a sequence of date-time
   objects representing the starting instant of the weeks
   in the same timezone that lie within the interval"
  ([i]
     (lazy-seq
      (let [start (decimate :week (start i))
            newstart (plus start (weeks 1))]
        (if (before? newstart (end i))
          (cons start (interval->days (interval newstart (end i))))
          (cons start nil))))))


;; ==================================================================
;;
;; Basic interface, relies on embedded event template and dispatch
;; 

(defn schedule-dispatcher
  ([schedule & rest]
     (assert (= (:type schedule) :schedule))
     (keyword (:stype schedule))))

(defmulti events schedule-dispatcher)

;;
;; Scheduler types
;;

;; Daily
;; Every day, one or more fixed times a day
;; With jitter

(defn events-within [interval events]
  (filter (comp (partial within? interval) :start) events))

(defn daily-events [schedule day-dt]
  (assert (= (:stype schedule) :daily))
  (for [{:keys [hour min jitter] :or {min 0 jitter 0}} (:times schedule)]
    (assoc (:event schedule)
      :start (plus day-dt (hours hour) (minutes min))
      :jitter jitter)))
  
(defmethod events :daily [schedule & [interval]]
  (assert (:times schedule))
  (->> (interval->days interval)
       (mapcat (partial daily-events schedule))
       (events-within interval)
       (map jitter-event)))

;; Weekly
;; Events for one or more days a week
;; :times record for each day-of-week + time of day

(defn weekly-events [schedule week-dt]
  (for [{:keys [day hour min jitter]} (:times schedule)]
    (assoc (:event schedule)
      :start (.withDayOfWeek (plus week-dt (hours hour) (minutes min)) day)
      :jitter jitter)))

(defmethod events :weekly [schedule interval]
  (assert (:times schedule))
  (->> (interval->weeks interval)
       (mapcat (partial weekly-events schedule))
       (events-within interval)
       (map jitter-event)))

