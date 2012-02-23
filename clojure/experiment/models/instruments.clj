(ns experiment.models.instruments
  (:use experiment.infra.models
        experiment.models.samples)
  (:require [clj-time.core :as time]
            [experiment.libs.datetime :as dt]
            [experiment.libs.zeo :as zeo]
            [experiment.libs.withings :as wg]
            [experiment.libs.strava :as strava]
            [experiment.libs.rescuetime :as rt]))

;;
;; Big ol file of instruments
;;
;; Connect 3rd party libraries to a mongo log and sql-based sample
;; set and provide an api to query instruments
;;

(defn get-instruments []
  (fetch-models :instrument))

(defn get-instrument [ref]
  (or (fetch-model "instrument" :where {:_id ref})
      (fetch-model "instrument" :where {:src ref})
      (fetch-model "instrument" :where {:name ref})))

;; Instrument Protocol
;; - last-update-received
;; - time-series (start, end, filter?)
;; - number-of-missed-records (start, end)
;; - refresh (pull from upstream server service)
;; - recompute (update time series DB, any derived measures)

(def ih (make-hierarchy))

(defmulti configured? (fn [i u] (keyword (:src i))) :hierarchy #'ih)
(defmulti last-update (fn [i u] (keyword (:src i))) :hierarchy #'ih)
(defmulti refresh (fn [i u] (keyword (:src i))) :hierarchy #'ih)
(defmulti reset (fn [i u] (keyword (:src i))) :hierarchy #'ih)
(defmulti time-series (fn [i u & args] (keyword (:src i))) :hierarchy #'ih)

;; Defaults

(defmethod last-update :default [instrument user]
  (get-last-sample-time user instrument))

(defmethod time-series :default [inst user & [start end convert?]]
  (refresh inst user)
  (get-samples user inst start end (if (nil? convert?) true convert?)))

(defmethod refresh :default [inst user]
  nil)

(defmethod reset :default [inst user]
  (reset-samples user inst))

;; Utils

(defn- stale? [inst user]
  (let [lu (last-update inst user)]
    (not (and lu
              (time/after?
               (time/minus (dt/now) (time/secs (or (:update-interval inst) 3600)))
               lu)))))
 
;; ------------------------------------
;; Rescuetime-based Instruments
;; ------------------------------------

(defmethod configured? :rt [inst user]
  (and (:rt-key user) (:rt-user user)))

;; Social Media Usage on Rescuetime (value in seconds)

(defmacro rt-update [inst user force? [var] & body]
  `(let [i# ~inst
         u# ~user]
     (when (or ~force? (stale? i# u#))
       (rt/with-key (:rt-api u#)
         (let [~var (or (last-update i# u#) (dt/from-utc 0))]
           ~@body)))))


(alter-var-root #'ih derive :rt-socmed-usage :rt)
(defmethod refresh :rt-socmed-usage
  [inst user & [force?]]
  (when (or force? (not (stale? inst user)))
    (rt/with-key (:rt-api user)
      (let [start (or (last-update inst user) (dt/from-utc 0))
            data (rt/social-media "day" start (dt/now))]
        (add-samples user inst (:rows data))))))

(defn- efficiency->total [[dt total people eff]]
  [(dt/from-iso-8601 dt) total])

(defn- efficiency->eff [[dt total people eff]]
  [(dt/from-iso-8601 dt) eff])

(alter-var-root #'ih derive :rt-efficiency :rt)
(defmethod refresh :rt-efficiency
  [inst user & [force?]]
  (rt-update inst user force? [start]
     (let [data (rt/efficiency "day" start (dt/now))]
       (add-samples user
                    inst
                    (map efficiency->eff (:rows data)))
       (add-samples user
                    (get-instrument-by-src "rt-total")
                    (map efficiency->total (:rows data))))))

(alter-var-root #'ih derive :rt-total :rt)
(defmethod refresh :rt-total
  [inst user]
  (refresh (get-instrument-by-src "rt-efficiency") user))

(defn ensure-rt-instruments []
  (when (not (get-instrument-by-src :rt-socmed-usage))
    (create-model!
     {:type :instrument
      :src :rt-socmed-usage
      :variable "Social Media Usage"
      :nicknames ["social media" "usage"]
      :comments []
      :description "Your social media usage as measured by rescuetime"}))
  (when (not (get-instrument-by-src :rt-efficiency))
    (create-model!
     {:type :instrument
      :src :rt-efficiency
      :variable "Computer Work Efficiency"
      :nicknames ["online efficiency" "work efficiency"]
      :comments []
      :description "Your computer-based work efficiency as measured by rescuetime"}))
  (when (not (get-instrument-by-src :rt-total))
    (create-model!
     {:type :instrument
      :src :rt-total
      :variable "Total time on computers"
      :nicknames ["computer time"]
      :comments []
      :description "Total time on a rescutime enabled computer"}))
  true)
          
  