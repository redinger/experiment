(ns experiment.views.charts
  (:use
   experiment.libs.datetime
   experiment.models.events
   experiment.infra.models
   handlebars.templates
   noir.core
   hiccup.page-helpers)
  (:require
   [noir.response :as response]
   [experiment.infra.session :as session]
   [somnium.congomongo :as mongo]))


(deftemplate highchart-div
  [:div {:class "highchart"}
   (%strcat "<div id='" (% cssid) "' style='height:" (% height) ";width:" (% width) ";'/>")])
    
(defn- timeseries-config [title type series]
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

(defn- lookup-trackers [instrument start]
  (fetch-models :tracker :where {:instrument.$id (:_id instrument)
				 :user.$id (:_id (session/current-user))
				 :start {:$gt start}}))

(defn- get-instrument [inst]
  (fetch-model :instrument
	       :where {:_id (mongo/object-id inst)}))

;;(defmulti tracker-chart :measure)

(defn tracker-chart [inst start end]
  (let [trackers (lookup-trackers inst (or start 0))
	series (sort-by first (apply concat (map :data trackers)))]
    (timeseries-config (:variable inst) "spline"
      [{:name (:variable inst)
	:data (vec series)}])))

(defn as-int [value]
  (try
    (Integer/parseInt value)
    (catch java.lang.Throwable e
      nil)))

(defpage event-chart-api [:get "/api/charts/tracker"] {:keys [inst start end] :as options}
  (let [instrument (get-instrument inst)]
    (response/json
     (tracker-chart instrument (or (as-int start) 0) (or (as-int end) (as-utc (now)))))))


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

