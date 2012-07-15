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
     (.appendDayOfWeekShortText)
     (.appendLiteral " ")
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

(def ^{:private true} rt-format
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

(def ^{:private true} iso-format
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
     (.appendSecondOfMinute 2)
     (.appendLiteral ".")
     (.appendMillisOfSecond 3)
     (.appendTimeZoneOffset "Z" true 2 2))))
     

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
  (time/to-time-zone (time/now) (mid/server-timezone)))

(def ^{:private true} short-fmt-intervals
  [[(time/days 1) short-today-fmt]
   [(time/weeks 1) short-week-fmt]
   [(time/years 1) short-year-fmt]
   [(time/years 1000) short-fmt]])

(defprotocol DateTime
  (date? [dt])
  (timezone [dt])
  (as-joda [dt])
  (as-java [dt]))

(defn- within-period-ago? [dt current period]
  (time/within? (time/interval (time/minus current period) current) dt))

(defn- short-formatter [dt]
  (let [cur (now)]
    (second
     (first
      (filter (fn [[period fmt]]
		(within-period-ago? dt cur period))
	      short-fmt-intervals)))))

(defn ago [f amount]
  (time/minus (now) (f amount)))

(defn a-month-ago []
  (ago time/months 1))

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
  ([utc] (when utc
           (from-utc utc (mid/session-timezone))))
  ([utc tz] (when utc
              (time/to-time-zone
               (coerce/from-long utc)
               tz))))

(defn from-iso
  "Simplified ISO formatted date/times"
  ([string]
     (from-iso string (mid/session-timezone)))
  ([string tz]
     (when (and string tz)
       (time/from-time-zone
        (.parseDateTime iso-format string)
        tz))))

(defn from-rt
  "Simplified ISO formatted date/times"
  ([string]
     (from-rt string (mid/session-timezone)))
  ([string tz]
     (when (and string tz)
       (time/from-time-zone
        (.parseDateTime rt-format string)
        tz))))

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
  (cond
   (date? dt)
   (coerce/to-long (as-joda dt))
   (= (type dt) java.lang.Long)
   dt
   true nil))

(defn as-date [dt]
  (cond
   (date? dt)
   (as-java dt)
   (= (type dt) java.lang.Long)
   (java.util.Date. dt)))

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

(defn as-iso [dt]
  (when dt
    (.print iso-format dt)))

(defn as-blog-date [dt]
  (when dt
    (.print blog-fmt dt)))

(defn in-server-tz [dt]
  (time/to-time-zone dt (time/default-time-zone)))

(defn in-default-tz [dt]
  (in-server-tz dt))

(defn in-session-tz [dt]
  (time/to-time-zone dt (mid/session-timezone)))

(defn to-session-tz [dt]
  (time/from-time-zone dt (mid/session-timezone)))

(defn wall-time [dt]
  (when dt (.toString (.toLocalTime dt) "KK:mm aa")))
   
;; Canonical Time Interface

(extend-protocol DateTime 
  java.util.Date
  (:date? [dt] true)
  (:as-joda [dt] (org.joda.time.DateTime. dt))
  (:as-java [dt] dt)
  (:timezone [dt] (timezone (as-joda dt)))
  org.joda.time.DateTime
  (:date? [dt] true)
  (:as-joda [dt] dt)
  (:as-java [dt] (.toDate dt))
  (:timezone [dt] (.getZone (.getChronology dt)))
  Object
  (:date? [dt] false)
  (:as-joda [dt] (throw (java.lang.UnsupportedOperationException. "Not a date object")))
  (:as-java [dt] (throw (java.lang.UnsupportedOperationException. "Not a date object")))
  (:timezone [dt] (throw (java.lang.UnsupportedOperationException. "Not a date object")))
  nil
  (:date? [dt] false)
  (:as-joda [dt] (throw (java.lang.UnsupportedOperationException. "Not a date object")))
  (:as-java [dt] (throw (java.lang.UnsupportedOperationException. "Not a date object")))
  (:timezone [dt] (throw (java.lang.UnsupportedOperationException. "Not a date object"))))
  
;; Make it easier to use intervals or explicit start/end dates          
(defmacro with-interval [[interval start end & [default-start default-end]] & body]
  `(let [interval# ~interval
         ~start (or (and interval# (.getStart interval#))
                    ~default-start
                    (time/minus (dt/now) (time/days 30)))
         ~end (or (and interval# (.getEnd interval#))
                  ~default-end
                  (dt/now))]
     ~@body))

          
