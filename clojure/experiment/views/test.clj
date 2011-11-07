(ns experiment.views.test
  (:require
   [experiment.views.common :as common]
   [noir.response :as response]
   [noir.request :as request]
   [clojure.data.json :as json]
   [somnium.congomongo :as mongo])
  (:use noir.core
	experiment.infra.models
        hiccup.core
        hiccup.page-helpers))

(defpage "/test" {}
  (common/layout
   [:script {:type "text/x-jquery-html" :id "color-box-template"}
    [:div {:style "margin: 20px; float: left; height: {{height}}px; width: {{width}}px; background-color: {{color}}"}]]
   [:script {:id "configmodel-bootstrap"}
    (clj-json.core/generate-string
     (export-model (fetch-models "configmodel")))]
   [:h1 "Home Page"]
   [:div {:id "config-app"}
    [:div {:id "config-input" :style "padding: 20px"}
     [:div {:data-role "fieldcontain"}
      [:label {:for "text"}
       "Enter color value (ex. red, green)"]
      [:input {:id "color-input" :type "text" :value "blue"}]]
     [:div {:data-role "fieldcontain"}
      [:label {:for "text"}
       "Enter width of the box (ex. 50, 60)"]
      [:input {:id "width-input" :type "text" :value "100"}]]
     [:div {:data-role "fieldcontain"}
      [:input {:id "save-forms" :type "submit" :value "Save"}]
      [:input {:id "load-forms" :type "submit" :value "Load"}]]]
    [:div {:id "color-boxes" :style "display: inline; list-style-type:none; width: 0%"}]]))

