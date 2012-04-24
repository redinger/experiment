(ns experiment.views.charts
  (:use
   experiment.infra.models
   experiment.models.events
   experiment.models.instruments
   clojure.math.numeric-tower
   handlebars.templates
   hiccup.page-helpers
   noir.core)
  (:require
   [clojure.tools.logging :as log]
   [experiment.views.common :as common]
   [experiment.libs.datetime :as dt]
   [clj-time.core :as time]
   [noir.response :as response]
   [experiment.infra.session :as session]
   [somnium.congomongo :as mongo]))


(defn as-utc-series [series]
  (vec
   (map (fn [point]
          (assoc point :ts (dt/as-utc (:ts point))))
        series)))

(defn test-regions [start end]
  (let [start (dt/as-utc start)
        end (dt/as-utc end)]
    [{:start (+ start (/ (- end start)  4))
      :end (+ start (* 2 (/ (- end start)  4)))
      :label "On antibiotics"}]))

(defn tracker-chart
  ([inst start end user]
     {:series (as-utc-series
               (time-series inst user start end false))
      :start (dt/as-utc start)
      :end (dt/as-utc end)
      :dataMin (min-plot inst)
      :dataMax (max-plot inst)})
;;      :regions (test-regions start end)})
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
     (tracker-chart instrument
                    (or (dt/from-iso-8601 start)
                        (time/minus (dt/now) (time/months 1)))
                    (or (dt/from-iso-8601 end)
                        (dt/now))))))


;; =========================
;; Control chart
;; =========================

;;(defn control-lines [control total]
;;   (let [mean (incanter.stats/mean control)
;; 	sd (incanter.stats/sd control)
;; 	mean-series {:name "Mean" :data (vec (repeat total mean))}
;; 	ucl-series {:name "UCL" :data (vec (repeat total (+ mean (* 2.5 sd))))}
;; 	lcl-series {:name "LCL" :data (vec (repeat total (- mean (* 2.5 sd))))}]
;;     (list ucl-series mean-series lcl-series)))

;;(defn control-points [userdata]
;;  (map last (filter (fn [entry] (= "Neither" (nth entry 2))) userdata)))
;;  (map last (first (partition-by #(nth % 2) userdata))))

;;(defn control-chart [experiment instrument]
;;  (let [

(defpage test-chart [:get "/dev/charts"] {:keys [] :as options}
  (common/layout
   ""
   (common/default-nav)
   [:div#test {:style "border: 1px solid black;"}]))
