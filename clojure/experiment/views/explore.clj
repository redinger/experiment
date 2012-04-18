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


(defpage "/explore*" {:as options}
  (page-frame
   ["Explore Experiments"
    :fixed-size 100
    :deps ["views/common", "views/explore"]]
   (nav-fixed (:nav (default-nav "explore")))
   [:div.container {:style "min-height: 400px"}
    [:div#main]]))

(deftemplate explore-home
  [:form {:class "well form-search"}
   (text-field {:class "search-query input-xlarge"}
               "q" (% query))
   [:button {:type "submit" :class "btn"} "Search"]])
               
   
;;    [:form {:class "well form-search"}
;;     [:input {:type "text"  :class "input-xlarge search-query"}]
;;     [:button {:type "submit" :class "btn"} "Search"]]]))
