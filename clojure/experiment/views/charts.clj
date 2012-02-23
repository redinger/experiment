(ns experiment.views.charts
  (:use
   experiment.infra.models
   experiment.models.events
   experiment.models.instruments
   handlebars.templates
   hiccup.page-helpers
   noir.core)
  (:require
   [experiment.libs.datetime :as dt]
   [clj-time.core :as time]
   [noir.response :as response]
   [experiment.infra.session :as session]
   [somnium.congomongo :as mongo]))


(deftemplate highchart-div
  [:div
   [:div {:class "highchart"}
   (%strcat "<div id='" (% cssid) "' style='height:" (% height) ";width:" (% width) ";'/>")]
   [:div {:class "d3chart"}]])
   
    
(defn- timeseries-config [title type series]
;;  (let [[min max] (compute-limits series)]
    {:chart {:type type}
     :legend {:enabled false}
     :plotOptions {:series {:animation false
                            :marker {:enabled false}}}
     :xAxis {:type "datetime"
             :dateTimeLabelFormats {:month "%e. %b" :year "%b"}}
     :yAxis {:type "linear" :title {:text title} :min 0 :max 7}
     :title {:text ""}
     :credits {:enabled false}
     ;;   :labels {:items [{:html "<div><p><b>Start</b></p></div>" :style {:left "100px" :top "100px"}}]}
     :series series})

(defn tracker-chart [inst start end]
  (let [series (time-series inst (session/current-user) start end false)]
    (clojure.tools.logging/spy series)
    (timeseries-config (:variable inst) "spline"
      [{:name (:variable inst)
	:data (vec series)}])))

(defn as-int [value]
  (try
    (Integer/parseInt value)
    (catch java.lang.Throwable e
      nil)))

(defn- a-month-ago []
  (time/minus (dt/now) (time/months 1)))

(defpage event-chart-api [:get "/api/charts/tracker"] {:keys [inst start end] :as options}
  (let [instrument (get-instrument (deserialize-id inst))]
    (response/json
     (tracker-chart instrument
                    (or (as-int start) (as-utc (a-month-ago)))
                    (or (as-int end) (as-utc (now)))))))


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

