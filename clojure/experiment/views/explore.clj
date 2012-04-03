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
   ["Explore Experiments"]
   (nav-fixed (:nav (default-nav "explore")))
   [:div.container
    [:div.page-header
     [:h1 "Browse and Search Experiments (COMING SOON)"]]]))
