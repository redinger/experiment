(ns experiment.views.home
  (:require
   [experiment.views.common :as common]
   [clojure.data.json :as json]
   [somnium.congomongo :as mongo])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers))

(defpage "/" {}
  (common/layout
   [:h1 ""]))


