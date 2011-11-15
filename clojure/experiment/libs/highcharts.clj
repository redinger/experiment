(ns experiment.libs.highcharts
  (:use noir.core
        hiccup.core
	handlebars.templates)
  (:require [clojure.data.json :as json]))

(defn default-config [id title type series]
  {:chart {:type type
	   :renderTo id}
   :plotOptions {:series {:animation false
			  :marker {:enabled false}}}
   :title {:text title}
   :credits {:enabled false}
   :labels {:items [{:html "<div><p><b>Start</b></p></div>" :style {:left "100px" :top "100px"}}]}
   :series series})

  