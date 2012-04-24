(ns experiment.views.explore
  (:use
   noir.core
   hiccup.core
   hiccup.page-helpers
   hiccup.form-helpers
   experiment.models.suggestions
   experiment.views.common
   experiment.views.menu
   handlebars.templates)
  (:require
   [cheshire.core :as json]
   [noir.response :as resp]
   [experiment.infra.session :as session]
   [experiment.infra.models :as models]
   [experiment.models.user :as user]
   [experiment.models.profile]
   [experiment.models.events :as events]
   [experiment.views.trials :as trials]))


(defpage "/explore/*" {:as options}
  (page-frame
   ["Explore Experiments"
    :fixed-size 50
    :deps ["views/common", "views/explore"]]
   (nav-fixed (:nav (default-nav "explore")))
   [:div.container {:style "min-height: 400px"}
    [:div#explore]]
   (render-all-templates)))
   
(defpage "/explore" {:as options}
  (resp/redirect "/explore/search"))

(deftemplate search-header
  [:div {:class "well search-box"}
   (text-field {:class "search-query input-xlarge"} "q" (% this))
   [:button {:type "button" :class "btn search-btn"} "Search"]])
               
   
;;    [:form {:class "well form-search"}
;;     [:input {:type "text"  :class "input-xlarge search-query"}]
;;     [:button {:type "submit" :class "btn"} "Search"]]]))
