(ns experiment.views.test
  (:require
   [experiment.views.common :as common]
   [noir.response :as response]
   [noir.request :as request])
  (:use noir.core
        experiment.infra.models
        hiccup.core
        hiccup.page-helpers))

(defpage "/test" {}
  (common/layout
   ["Test Page"
    (common/default-nav)]
   [:div.content
    [:div.page-header
     [:h1 "Test Page"]
     [:p "This is a test"]]]))


