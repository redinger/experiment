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

(def ^:private short-fmt
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

(defn from-utc [utc]
  (coerce/from-long utc))

(defn from-iso-8601 [string]
  (fmt/parse string))

(defn as-utc [dt]
  (condp = (type dt)
      java.lang.Long dt
      org.joda.time.DateTime (coerce/to-long dt)
      java.util.Date (.getTime dt)))

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


(defn to-default-tz [dt]
  (time/to-time-zone dt (time/default-time-zone)))

