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

(deftemplate dashboard-header
  [:div
   [:ul
    [:li (%str "Number of Friends: " (% friends))]
    [:li (%str "Active Experiments: " (% experiments))]]])


;; ===========================
;; Trial View

;; Take a trial object and render it's header
(deftemplate trial-header
  [:div
   [:h1 (% experiment.title)]])
   

;; Application renders views of data   

;; ===========================
;; Search

(deftemplate search-filter
  [:div.search-filter
   [:input {:type "text"
	    :id "search-filter-input"
	    :value (% query)}]
   [:span {:class "filter-guide"}
    "<b>show</b> type, <b>with</b> treatment, <b>for</b> symptom/condition, <b>use</b> instrument &nbsp;" [:a {:href "#" :class "help-link"} "[More help...]"]]])

;; ===========================
;; Admin
  
(deftemplate admin-main
  [:div [:h1 "This is the admin template"]])


;; ===========================
;; Dialogs

(deftemplate basic-dialog
  [:div {:id "osx-modal-content"}
   [:div {:id "osx-modal-title"} "Help Dialog"]
   [:div {:class "close"}
    [:a {:href "#" :class "simplemodal-close"} "x"]]
   [:div {:id "osx-modal-data"}
    [:h2 (% title)]
    [:p (% body)]]])
