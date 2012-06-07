(ns experiment.views.charts
  (:use
   experiment.infra.models
   experiment.models.trial
   experiment.models.events
   experiment.models.schedule
   experiment.models.instruments
   clojure.math.numeric-tower
   handlebars.templates
   hiccup.page-helpers
   noir.core)
  (:require
   [incanter.stats]
   [clojure.tools.logging :as log]
   [clojure.walk :as walk]
   [experiment.views.common :as common]
   [experiment.libs.datetime :as dt]
   [clj-time.core :as time]
   [noir.response :as response]
   [experiment.infra.session :as session]
   [somnium.congomongo :as mongo]))

(defn convert-to-utc [node]
  (if-let [utc (dt/as-utc node)]
    utc
    node))

(defn as-utc-dataset [dataset]
  (walk/postwalk convert-to-utc dataset))


(defn test-regions [start end]
  (let [start (dt/as-utc start)
        end (dt/as-utc end)]
    [{:start (+ start (/ (- end start)  4))
      :end (+ start (* 2 (/ (- end start)  4)))
      :label "On antibiotics"}]))


(defn tracker-chart
  ([inst start end user]
     {:series
      [{:data (time-series inst user start end false)
        :start start
        :end end
        :dataMin (min-plot inst)
        :dataMax (max-plot inst)}]})
  ([inst start end]
     (tracker-chart inst start end (session/current-user))))

(defn as-int [value]
  (try
    (Integer/parseInt value)
    (catch java.lang.Throwable e
      nil)))

(defpage event-chart-api [:get "/api/charts/tracker"] {:keys [inst start end] :as options}
  (let [instrument (get-instrument (deserialize-id inst))]
    (response/json
     (as-utc-dataset
      (tracker-chart instrument
                     (or (dt/from-iso-8601 start)
                         (time/minus (dt/now) (time/months 1)))
                     (or (dt/from-iso-8601 end)
                         (dt/now)))))))
  

;; Control chart
;; ----------------------------

;; ### Add control lines to a run chart

(defn pair-point
  "Convert point rec into point pair"
  [point-rec]
  [(:ts point-rec) (:v point-rec)])

(defn series-values
  "Convert a record series to canonical value seq"
  [series]
  (let [data (:data series)]
    (assert (map? (first data)))
    (map :v data)))

(defn point-in-period [point period]
  (.contains (:interval period) (:ts point)))

(defn points-in-periods [periods points]
  (filter #(some (partial point-in-period %) periods) points))

(def testing* nil)

(defn baseline-values [chart trial]
  (alter-var-root #'testing* (fn [old] [chart trial]))
  (map :v
       (-> (trial-periods trial)
           (baseline-periods (time/interval (:start chart) (:end chart)))
           (points-in-periods (:data chart)))))

(defn control-lines
  "Given a chart (outcome sequence as first series), compute the UCL and LCL
   bounds over baseline data (if sufficient is present) using the trial schedule
   to get time regions"
  [series trial]
  (if (< (count (:data series)) 1)
    series
    (let [yvalues (baseline-values series trial)]
      (if (> (count yvalues) 1)
        (let [mean (incanter.stats/mean yvalues)
              sd (incanter.stats/sd yvalues)
              ucl (min (+ mean (* 3 sd)) (or (:dataMax series) (apply max yvalues)))
              lcl (max (- mean (* 3 sd)) 0.1)]
          (assoc series
            :ctrl {:lcl lcl :ucl ucl :mean mean}))
        series))))

;; ### Add regions to the chart

(defn period-as-region [series period]
  (let [i (:interval period)
        rbegin (.getStart i)
        rend (.getEnd i)
        start (:start series)
        end (:end series)
        rbegin (if (time/after? start rbegin) start rbegin)
        rend (if (time/before? end rend) end rend)]
    (merge 
     {:type "area"
      :start rbegin
      :end rend}
     (case (:label period)
       "base"
       {:color "blue"
        :label "Baseline"}
       "treat"
       {:color "red"
        :label "Treatment"}))))

(defn treatment-regions [series trial]
  (if-let [regions
           (map (partial period-as-region series)
                (trial-periods
                 trial 
                 (time/interval (:start series) (:end series))))]
    (assoc series :regions (vec regions))
    series))

;; ### Highlight significant events

(defn- significant-point [point]
  (merge point {:type "point"}))

(defn outside-control? [upper lower points]
  (filter (fn [point]
            (or (> (:v point) upper)
                (< (:v point) lower)))
          points))

(defn- significant-seq [seq]
  {:type "sequence"
   :points (vec seq)})

(defn significant-sequences [mean points]
  (letfn [(polarity? [point]
            (cond (> (:v point) mean) :pos
                  (< (:v point) mean) :neg
                  true nil))]
    (when (number? (:v (first points)))
      (loop [points points
             seq '()
             last nil
             seqs '()]
        (let [point (first points)]
          (cond (empty? points) (vec (map vec (reverse seqs)))
                (empty? seq) (if-let [polarity (polarity? point)]
                               (recur (rest points) (list point) (polarity? point) seqs)
                               (recur (rest points) '() nil seqs))
                (< (count seq) 5) (if (= (polarity? point) last)
                                    (recur (rest points) (cons point seq) last seqs)
                                    (recur (rest points) (list point) (polarity? point) seqs))
                true (recur (rest points) '() nil (cons (reverse seq) seqs))))))))

(defn significant-events [series]
  (if-let [ctrl (:ctrl series)]
    (let [{ucl :ucl lcl :lcl mean :mean} ctrl
          points (:data series)
          sig-points (map significant-point
                      (outside-control? ucl lcl points))
          sig-seqs (map significant-seq
                        (significant-sequences mean points))]
      (assoc series
        :significance (vec (concat sig-points sig-seqs))))
    series))

;; ### Control chart is tracker + ...
;; Control lines, Regions, Annotations

(defn control-chart [trial start end]
  (let [chart (tracker-chart (trial-outcome trial)
                             (or start
                                 (time/minus (dt/now) (time/months 1)))
                             (or end
                                 (dt/now)))]
    {:series [(-> (first (:series chart))
                  (control-lines trial)
                  (treatment-regions trial)
                  (significant-events))]}))

(defpage ctrl-chart-api [:get "/api/charts/trial"]
  {:keys [id start end] :as options}
  (let [trial (lookup-trial id)]
    (response/json
     (as-utc-dataset
      (control-chart trial (dt/from-iso-8601 start) (dt/from-iso-8601 end))))))

;;     (event-chart-api
;;      {:inst (str (.getId (first (:outcome (trial-experiment trial)))))
;;       :start start
;;       :end end})))
