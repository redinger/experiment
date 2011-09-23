(ns experiment.views.welcome
  (:require [experiment.views.common :as common])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers))

(defpartial welcome-msg [user exp]
  [:div#welcome
   [:p "Welcome " user ", to experiment '" exp "'"]])
  
(defpage "/welcome" {:keys [user exp]}
  (common/layout
   [:h1 "Experiments"]
   (welcome-msg user exp)))


