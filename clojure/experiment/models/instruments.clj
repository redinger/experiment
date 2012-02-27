(ns experiment.models.instruments
  (:use experiment.infra.models
        experiment.models.samples2
        clojure.math.numeric-tower)
  (:require [clj-time.core :as time]
            [experiment.models.user :as user]
            [experiment.libs.datetime :as dt]
            [experiment.libs.zeo :as zeo]
            [experiment.libs.withings :as wi]
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
(defmulti refresh (fn [i u & args] (keyword (:src i))) :hierarchy #'ih)
(defmulti reset (fn [i u] (keyword (:src i))) :hierarchy #'ih)
(defmulti time-series (fn [i u & args] (keyword (:src i))) :hierarchy #'ih)

;; Utils

(defn- sample-interval [inst]
  (if-let [mins (get-in inst [:sampling :period])]
    (time/minutes mins)
    (time/days 1)))

(defn- sample->pair [sample]
  [(:ts sample) (:v sample)])

(defn- stale?
  "Data hasn't been feched, or hasn't been updated after
   a certain time ago, 1 hour by default"
  [inst user]
  (let [lu (last-update inst user)]
    (or (not lu)
        (time/after?
         (time/minus (dt/now) (sample-interval inst))
         lu))))
 
;; Defaults

(defmethod last-update :default [instrument user]
  (last-updated-time user instrument))

(defmethod time-series :default [inst user & [start end convert?]]
  (refresh inst user)
  (map sample->pair
       (get-samples user inst
                    :start (or start (dt/a-month-ago))
                    :end (or end (dt/now)))))

(defmethod refresh :default [inst user & [force?]]
  nil)

(defmethod reset :default [inst user]
  (reset-samples user inst))

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
         (let [~var (or (last-update i# u#) ~force? (dt/a-month-ago))]
           ~@body))
       true)))

(defn seconds-to-hours [seconds]
  (assert (number? seconds))
  (/ (round (* (/ seconds 3600) 100)) 100.0))

(defn- socmed->series [[dt time people cat]]
  {:ts (dt/from-iso-8601 dt) :v (seconds-to-hours time) :secs time})

(alter-var-root #'ih derive :rt-socmed-usage :rt)
(defmethod refresh :rt-socmed-usage
  [inst user & [force?]]
  (rt-update inst user force? [start]
     (let [data (rt/social-media "day" start (dt/now))]
       (add-samples user inst
                    (map socmed->series (:rows data))))))

(defn- efficiency->total [[dt total people eff]]
  {:ts (dt/from-iso-8601 dt) :v (seconds-to-hours total) :secs total})

(defn- efficiency->eff [[dt total people eff]]
  {:ts (dt/from-iso-8601 dt) :v eff})

(alter-var-root #'ih derive :rt-efficiency :rt)
(defmethod refresh :rt-efficiency
  [inst user & [force?]]
  (rt-update inst user force? [start]
     (let [data (rt/efficiency "day" start (dt/now))]
       (add-samples user
                    inst
                    (map efficiency->eff (:rows data)))
       (add-samples user
                    (get-instrument "rt-total")
                    (map efficiency->total (:rows data))))))

(alter-var-root #'ih derive :rt-total :rt)
(defmethod refresh :rt-total
  [inst user & [force?]]
  (refresh (get-instrument "rt-efficiency") user force?))

(defmacro ensure-instrument [[svc type] & body]
  (assert (keyword type))
  `(when (not (get-instrument ~type))
     (create-model!
      (merge
       {:type :instrument
        :src ~type
        :svc ~svc
        :comments []}
       ~@body))))

(defn ensure-rt-instruments []
  (ensure-instrument [:rescuetime :rt-socmed-usage]
    {:variable "Social Media Usage"
     :nicknames ["social media" "usage"]
     :sampling {:period (* 60 12) ;; 12 hours
                :chunksize :month}
     :description "Your social media usage as measured by rescuetime"})
  (ensure-instrument [:rescuetime :rt-efficiency]
    {:variable "Computer Work Efficiency"
     :nicknames ["online efficiency" "work efficiency"]
     :sampling {:period (* 60 12) ;; 12 hours
                :chunksize :month}
     :description "Your computer-based work efficiency as measured by rescuetime"})
  (ensure-instrument [:rescuetime :rt-total]
    {:variable "Total time on computers"
     :nicknames ["computer time"]
     :sampling {:period (* 60 12) ;; 12 hours
                :chunksize :month}
     :description "Total time on a rescutime enabled computer"})
  true)
          
;; ------------------------------------------
;; Withings Instruments (scale only)
;; ------------------------------------------

(defmethod configured? :wt [inst user]
  (and (wi/get-access-token user)
       (wi/get-access-secret user)
       (wi/get-userid user)))

(defn wi-inst-by-type [type]
  (fetch-model :instrument :where {:src-type type}))

(defn wi-sample [sample]
  (let [{:keys [date type value]} sample]
    (println date)
    (if date
      {:ts (dt/from-epoch date) :v value}
      (println sample))))

(defn add-wi-group [user [type samples]]
  (add-samples
   user (wi-inst-by-type type)
   (keep wi-sample samples)))

(defmethod refresh :wi
  [inst user & [force?]]
  (when (or force? (stale? inst user))
    (map (partial add-wi-group user)
         (group-by
          :type
          (second (wi/user-measures user (or (last-update inst user)
                                             force?
                                             (time/epoch))))))
    true))

(def wi-instruments
  [:wi-weight :wi-height :wi-lbm :wi-fat-ratio :wi-fat-mass])
            
(dorun
  (map (fn [iname]
         (alter-var-root #'ih derive iname :wi))
       wi-instruments))
            
(defn ensure-wi-instruments []
  (ensure-instrument [:withings :wi-weight]
    {:variable "Weight"
     :src-type :weight
     :nicknames ["withing weight" "scale weight"]
     :description "Weight as measured by the withings scale"})
  (ensure-instrument [:withings :wi-height]
    {:variable "Height"
     :src-type :height
     :nicknames ["height"]
     :description ""})
  (ensure-instrument [:withings :wi-lbm]
    {:variable "LBM"
     :src-type :lbm
     :nicknames ["lean body mass" "lbm"]
     :description "Lean Body Mass according to Withings"})
  (ensure-instrument [:withings :wi-fat-ratio]
    {:variable "Fat Ratio"
     :src-type :fat-ratio
     :nicknames ["fat %" "fat ratio"]
     :description ""})
  (ensure-instrument [:withings :wi-fat-mass]
    {:variable "Fat Mass"
     :src-type :fat-mass
     :nicknames ["fat mass" "fat"]
     :description "Fat Mass according to Withings Scale"})
  true)
     
;; ------------------------------------------
;; Bootstrapping
;; ------------------------------------------

(defn ensure-instruments []
  (ensure-rt-instruments)
  (ensure-wi-instruments))
  