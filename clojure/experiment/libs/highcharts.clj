(ns experiment.libs.highcharts
  (:use noir.core
        hiccup.core
	handlebars.templates)
  (:require [clojure.data.json :as json]))

(deftemplate highchart-div
  [:div {:class "highchart"}
   [:div {:id (% cssid) :style (%strcat "height: " (% height) "px; width: " (% width) "px;")}]])
    
(defn default-config [title type series]
  {:chart {:type type}
   :plotOptions {:series {:animation false
			  :marker {:enabled false}}}
   :title {:text title}
   :credits {:enabled false}
   :labels {:items [{:html "<div><p><b>Start</b></p></div>" :style {:left "100px" :top "100px"}}]}
   :series series})

  