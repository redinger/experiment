(ns experiment.models.schedule
  (:use
   experiment.infra.models
   [clj-time.core :as time :exclude [extend]])
  (:require
   [experiment.models.user :as user]
   [experiment.libs.datetime :as dt]))


;; Scheduling
;; -----------------------
;; 
;; Methods to convert a scheduling object associated with a tracker or trial
;; into a set of events dictated by the schedule's event template
;; 
;; Exports: (events <schedule> <interval>)
;;
;; Schedule Object:
;; {
;;  :type "schedule"
;;  :stype [ "daily" | "weekly | "periodic" ]
;;  :times <seconds>
;;  :event {...}
;; }


;; Scheduler utilities
;; --------------------------

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
       (assoc event
         :start (plus (:start event) (random-minutes jitter)))
       event)))

(defn interval->days
  "Given an interval of time, return a sequence of date-time
   objects representing the starting instant of the days
   that lie within the interval in the same timezone"
  ([i]
     (assert (= (type i) org.joda.time.Interval))
     (lazy-seq
      (let [start (decimate :day (dt/in-session-tz (start i)))
            newstart (plus start (days 1))]
        (if (before? newstart (end i))
          (cons start (interval->days (interval newstart (end i))))
          (cons start nil))))))

(defn interval->weeks
  "Given an interval of time, return a sequence of date-time
   objects representing the starting instant of the weeks
   in the same timezone that lie within the interval"
  ([i]
     (assert (= (type i) org.joda.time.Interval))
     (lazy-seq
      (let [start (decimate :week (start i))
            newstart (plus start (weeks 1))]
        (if (before? newstart (end i))
          (cons start (interval->days (interval newstart (end i))))
          (cons start nil))))))

(defn events-within [interval events]
  (filter (comp (partial within? interval) :start) events))


;;
;; Basic interface, relies on embedded event template and dispatch
;; ------------------------------------------------------------------

(defn schedule-dispatcher
  ([schedule & rest]
     (assert (= (:type schedule) "schedule"))
     (keyword (:stype schedule))))

(defmulti events schedule-dispatcher)

;;
;; Scheduler types
;; -------------------------------

;; Daily
;; Every day, one or more fixed times
;; :times - [{:hour :min}, ...] event times
;; :jitter +/- jitter minutes for each time spec

(defn daily-events [schedule day-dt]
  (assert (= (:stype schedule) "daily"))
  (let [day-dt (dt/to-session-tz day-dt)]
    (for [{:keys [hour min] :or {min 0}} (:times schedule)]
      (assoc (:event schedule)
        :status "pending"
        :start (dt/in-server-tz (plus day-dt (hours hour) (minutes min)))
        :jitter (:jitter schedule)))))
  
(defmethod events :daily [schedule & [interval]]
  (assert (:times schedule))
  (->> (interval->days interval)
       (mapcat (partial daily-events schedule))
       (events-within interval)
       (map jitter-event)))

;; Weekly
;; Events for one or more days a week
;; :times - [{:day :hour :min}, ...] event times
;; :jitter - +/- jitter minutes for each time

(defn weekly-events [schedule week-dt]
  (for [{:keys [day hour min]} (:times schedule)]
    (let [week-dt (dt/to-session-tz week-dt)
          day-dt (.withDayOfWeek (plus week-dt (hours hour) (minutes min)) day)]
      (assoc (:event schedule)
        :status "pending"
        :start (dt/in-server-tz day-dt)
        :jitter (:jitter schedule)))))

(defmethod events :weekly [schedule interval]
  (assert (:times schedule))
  (->> (interval->weeks interval)
       (mapcat (partial weekly-events schedule))
       (events-within interval)
       (map jitter-event)))

;; Periodic
;; Support periods schedules within a set of periods
;; :periods - list of intervals {:start dt :end dt}
;; :schedule - schedule to maintain during those intervals

(defn- periodic-record-intervals [rec]
  (update-in rec [:periods]
             (fn [orig]
               (map #(assoc %1 :interval (interval (:start %1) (:end %1))) orig))))
 
(defn period-overlaps? [inter period]
  (overlaps? inter (:interval period)))
  
(defmethod events :periodic [schedule interval]
  (assert (:periods schedule))
  (->> (:periods (periodic-record-as-joda schedule))
       (filter (partial period-overlaps? interval))
       (mapcat (fn [period] (events (:schedule schedule)
                                    (.overlap interval (:interval period)))))))
               
       

