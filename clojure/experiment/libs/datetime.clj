(ns experiment.libs.datetime
  (:require [clj-time.core :as time]
	    [clj-time.coerce :as coerce]
	    [clj-time.format :as fmt]
        [experiment.infra.middleware :as mid])
  (:import
   [org.joda.time.format DateTimeFormatterBuilder DateTimeFormatter]))

;; I Hate Dates and Times in ALL LANGUAGES

(def ^{:private true} short-today-fmt
  (.toFormatter
   (doto (DateTimeFormatterBuilder.)
     (.appendClockhourOfHalfday 1)
     (.appendLiteral ":")
     (.appendMinuteOfHour 2)
     (.appendHalfdayOfDayText)
     (.appendLiteral " Today ")
     (.appendTimeZoneShortName))))

(def ^{:private true} short-week-fmt
  (.toFormatter
   (doto (DateTimeFormatterBuilder.)
     (.appendClockhourOfHalfday 1)
     (.appendLiteral ":")
     (.appendMinuteOfHour 2)
     (.appendHalfdayOfDayText)
     (.appendLiteral " ")
     (.appendDayOfWeekShortText)
     (.appendLiteral " ")
     (.appendTimeZoneShortName))))

(def ^{:private true} short-year-fmt
  (.toFormatter
   (doto (DateTimeFormatterBuilder.)
     (.appendClockhourOfHalfday 1)
     (.appendLiteral ":")
     (.appendMinuteOfHour 2)
     (.appendHalfdayOfDayText)
     (.appendLiteral " ")
     (.appendMonthOfYearShortText)
     (.appendLiteral " ")
     (.appendDayOfMonth 1)
     (.appendLiteral " ")
     (.appendTimeZoneShortName))))

(def ^{:private true} blog-fmt
  (.toFormatter
   (doto (DateTimeFormatterBuilder.)
     (.appendMonthOfYearShortText)
     (.appendLiteral " ")
     (.appendDayOfMonth 1)
     (.appendLiteral ", ")
     (.appendYear 4 4)
     (.appendLiteral " at ")
     (.appendClockhourOfHalfday 1)
     (.appendLiteral ":")
     (.appendMinuteOfHour 2)
     (.appendLiteral " ")
     (.appendHalfdayOfDayText))))

(def ^{:private true} short-fmt
  (.toFormatter
   (doto (DateTimeFormatterBuilder.)
     (.appendClockhourOfHalfday 1)
     (.appendLiteral ":")
     (.appendMinuteOfHour 2)
     (.appendLiteral " ")
     (.appendHalfdayOfDayText)
     (.appendLiteral " ")
     (.appendMonthOfYearShortText)
     (.appendLiteral " ")
     (.appendDayOfMonth 1)
     (.appendLiteral ", ")
     (.appendYear 4 4)
     (.appendLiteral " ")
     (.appendTimeZoneShortName))))

(def ^{:private true} short-date
  (.toFormatter
   (doto (DateTimeFormatterBuilder.)
     (.appendMonthOfYearShortText)
     (.appendLiteral " ")
     (.appendDayOfMonth 1)
     (.appendLiteral ", ")
     (.appendYear 4 4)
     (.appendLiteral " ")
     (.appendTimeZoneShortName))))

(def ^{:private true} iso-8601-date
  (.toFormatter
   (doto (DateTimeFormatterBuilder.)
     (.appendYear 4 4)
     (.appendLiteral "-")
     (.appendMonthOfYear 2)
     (.appendLiteral "-")
     (.appendDayOfMonth 2))))

(def ^{:private true} iso-8601
  (org.joda.time.format.ISODateTimeFormat/basicDateTime))

  ;; (.toFormatter
  ;;  (doto (DateTimeFormatterBuilder.)
  ;;    (.appendYear 4 4)
  ;;    (.appendLiteral "-")
  ;;    (.appendMonthOfYear 2)
  ;;    (.appendLiteral "-")
  ;;    (.appendDayOfMonth 2)
  ;;    (.appendLiteral "T")
  ;;    (.appendHourOfDay 2)
  ;;    (.appendLiteral ":")
  ;;    (.appendMinuteOfHour 2)
  ;;    (.appendLiteral ":")
  ;;    (.appendSecondOfMinute 2)
  ;;    (.appendLiteral 

(def ^{:private true} time-fmt (fmt/formatter "h:mma" (time/default-time-zone)))
(def ^{:private true} date-fmt (fmt/formatter "MM/dd/yy" (time/default-time-zone)))

(defn now
  "Returns a date-time"
  []
  (time/to-time-zone (time/now) (mid/session-timezone)))

(def ^{:private true} short-fmt-intervals
  [[(time/days 1) short-today-fmt]
   [(time/weeks 1) short-week-fmt]
   [(time/years 1) short-year-fmt]
   [(time/years 1000) short-fmt]])

(defn- within-period-ago? [dt current period]
  (time/within? (time/interval (time/minus current period) current) dt))

(defn- short-formatter [dt]
  (let [cur (now)]
    (second
     (first
      (filter (fn [[period fmt]]
		(within-period-ago? dt cur period))
	      short-fmt-intervals)))))

(defn a-month-ago []
  (time/minus (time/now) (time/months 1)))

;; Canonicalize

(defmacro with-server-timezone
  [& body]
  `(binding [mid/*timezone* (mid/server-timezone)]
     ~@body))

(defmacro with-user-timezone
  [[user] & body]
  `(binding [experiment.infra.middleware/*current-user* ~user]
     (binding [mid/*timezone*
               (or (mid/user-timezone)
                   (mid/server-timezone))]
       ~@body)))

(defn from-utc
  ([utc] (from-utc utc (mid/session-timezone)))
  ([utc tz] (when utc (time/to-time-zone (from-utc utc) tz))))
  
(defn from-iso-8601
  ([string]
     (from-iso-8601 string (mid/session-timezone)))
  ([string tz]
     (when (and string tz)
       (time/from-time-zone 
        (.parseDateTime iso-8601 string)
        tz))))

(defn from-epoch
  ([epoch]
     (from-epoch epoch (mid/session-timezone)))
  ([epoch tz]
     (when (and (number? epoch) tz)
       (time/to-time-zone (time/plus (time/epoch) (time/secs epoch)) tz))))

(defn from-date
  ([date]
     (from-date date (mid/session-timezone)))
  ([date tz]
     (coerce/from-date date)))

;; Export formats

(defn as-utc [dt]
  (condp = (type dt)
      java.lang.Long dt
      org.joda.time.DateTime (coerce/to-long dt)
      java.util.Date (.getTime dt)))

(defn as-date [dt]
  (condp = (type dt)
    java.lang.Long (java.util.Date. dt)
    org.joda.time.DateTime (coerce/to-date dt)
    java.util.Date dt
    nil nil))

(defn as-short-relative-string [dt]
  (.print (short-formatter dt) dt))

(defn as-short-string [dt]
  (when dt
    (.print (second (last short-fmt-intervals)) dt)))

(defn as-short-date [dt]
  (when dt
    (.print short-date dt)))
	
(defn as-iso-8601-date [dt]
  (when dt
    (.print iso-8601-date dt)))

(defn as-iso-8601 [dt]
  (when dt
    (.print iso-8601 dt)))

(defn as-blog-date [dt]
  (when dt
    (.print blog-fmt dt)))

(defn in-server-tz [dt]
  (time/to-time-zone dt (time/default-time-zone)))

(defn in-default-tz [dt]
  (in-server-tz dt))

(defn in-session-tz [dt]
  (println (mid/server-timezone))
  (time/to-time-zone dt (mid/session-timezone)))

(defn to-session-tz [dt]
  (time/from-time-zone dt (mid/session-timezone)))