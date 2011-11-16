(ns experiment.views.pages
  (:require
   [clojure.data.json :as json]
   [noir.response :as resp]
   [experiment.infra.models :as models]
   [experiment.infra.session :as session]
   [experiment.models.user :as user]
   [experiment.models.core :as core]
   [experiment.views.common :as common])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers
	experiment.libs.highcharts
	handlebars.templates))

;; This file contains templates and other harness for differnet
;; page-level views for the main client application framework

;; ===========================
;; Dashboard

;;{:trials
;; :user
;; :tracking }

(deftemplate dashboard-header
  [:div.dashboard-header
   [:ul
    [:li [:a.tab.active-tab {:href "/app/dashboard/overview"} "Overview"]]
    [:li [:a.tab {:href "/app/dashboard/tracking"} "Tracking"]]
    [:li [:a.tab {:href "/app/dashboard/journal"} "Journal"]]]])

;; Application renders views of data   

;; ===========================
;; Search

(deftemplate search-filter
  [:div.search-filter
   [:input {:type "text"
	    :id "search-filter-input"
	    :value (% query)}]
   [:span.filter-guide
    "<b>show</b> type, <b>with</b> treatment, <b>for</b> symptom/condition, <b>use</b> instrument &nbsp;" [:a.help-link {:href "#"} "[More help...]"]]])

;; ===========================
;; Admin
  
(deftemplate admin-main
  [:div [:h1 "This is the admin template"]])


;; ===========================
;; Dialogs

(deftemplate basic-dialog
  [:div#osx-modal-content 
   [:div#.osx-modal-content "Help Dialog"]
   [:div.close
    [:a.simplemodal-close {:href "#"} "x"]]
   [:div#osx-modal-data
    [:h2 (% title)]
    [:p (% body)]]])
