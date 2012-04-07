(ns experiment.libs.datetime
  (:require [clj-time.core :as time]
	    [clj-time.coerce :as coerce]
	    [clj-time.format :as fmt])
  (:import
   [org.joda.time.format DateTimeFormatterBuilder DateTimeFormatter]))

;; I Hate Dates and Times in ALL LANGUAGES

(def ^:private short-today-fmt
  (.toFormatter
   (doto (DateTimeFormatterBuilder.)
     (.appendClockhourOfHalfday 1)
     (.appendLiteral ":")
     (.appendMinuteOfHour 2)
     (.appendHalfdayOfDayText)
     (.appendLiteral " Today ")
     (.appendTimeZoneShortName))))

(def ^:private short-week-fmt
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

(def ^:private short-year-fmt
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

(def ^:private blog-fmt
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

(def ^:private short-fmt
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

(def ^:private short-date
  (.toFormatter
   (doto (DateTimeFormatterBuilder.)
     (.appendMonthOfYearShortText)
     (.appendLiteral " ")
     (.appendDayOfMonth 1)
     (.appendLiteral ", ")
     (.appendYear 4 4)
     (.appendLiteral " ")
     (.appendTimeZoneShortName))))

(def ^:private iso-8601-date
  (.toFormatter
   (doto (DateTimeFormatterBuilder.)
     (.appendYear 4 4)
     (.appendLiteral "-")
     (.appendMonthOfYear 2)
     (.appendLiteral "-")
     (.appendDayOfMonth 2))))

(def ^:private iso-8601
  (.toFormatter
   (doto (DateTimeFormatterBuilder.)
     (.appendYear 4 4)
     (.appendLiteral "-")
     (.appendMonthOfYear 2)
     (.appendLiteral "-")
     (.appendDayOfMonth 2)
     (.appendLiteral "T")
     (.appendHourOfDay 2)
     (.appendLiteral ":")
     (.appendMinuteOfHour 2)
     (.appendLiteral ":")
     (.appendSecondOfMinute 2))))

(def ^:private time-fmt (fmt/formatter "h:mma" (time/default-time-zone)))
(def ^:private date-fmt (fmt/formatter "MM/dd/yy" (time/default-time-zone)))

(defn now
  "Returns a date-time"
  []
  (time/to-time-zone (time/now) (org.joda.time.DateTimeZone/getDefault)))

(def ^:private short-fmt-intervals
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

(defn from-utc
  ([utc] (when utc (coerce/from-long utc)))
  ([utc tz] (when utc (time/to-time-zone (from-utc utc) tz))))
  
(defn from-iso-8601 [string] (fmt/parse string))

(defn from-epoch
  ([epoch] (when (number? epoch) (time/plus (time/epoch) (time/secs epoch))))
  ([epoch tz] (when (and (number? epoch) tz)
                (time/to-time-zone (from-epoch epoch) tz))))

(defn from-date
  ([date] (coerce/from-date date)))

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
    java.util.Date dt))

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

(defn to-default-tz [dt]
  (time/to-time-zone dt (time/default-time-zone)))

