(ns experiment.views.charts
  (:use
   experiment.models.events
   experiment.infra.models
   handlebars.templates
   noir.core
   hiccup.page-helpers)
  (:require
   [clj-time.core :as time]
   [noir.response :as response]
   [experiment.infra.session :as session]
   [somnium.congomongo :as mongo]
   [clj-time.coerce :as coerce]))


(deftemplate highchart-div
  [:div {:class "highchart"}
   [:div {:id (% cssid) :style (%strcat "height: " (% height) "px; width: " (% width) "px;")}]])
    
(defn- default-config [title type series]
  {:chart {:type type}
   :plotOptions {:series {:animation false
			  :marker {:enabled false}}}
   :title {:text title}
   :credits {:enabled false}
   :labels {:items [{:html "<div><p><b>Start</b></p></div>" :style {:left "100px" :top "100px"}}]}
   :series series})

(defn- lookup-trackers [instid start]
  (fetch-models :tracker :where {:instrument.$id instid
				 :user.$id (:_id (session/current-user))
				 :end {:$gt start}}))

(defn- get-instrument [inst]
  (fetch-model :instrument
	       :where {:_id (mongo/object-id inst)}))

(defn event-chart [inst start]
  (let [trackers (lookup-trackers inst start)
	instrument (get-instrument inst)
	series (sort-by first (apply concat (map :data trackers)))]
    (default-config (:variable instrument) "spline"
      [{:name (:variable instrument)
	:data series}])))

(defpage event-chart-api [:get "/api/charts/tracker"] {:keys [inst start]}
  (response/json
   (event-chart inst start)))
