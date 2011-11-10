(ns experiment.views.pages
  (:require
   [clojure.data.json :as json]
   [noir.response :as resp]
   [experiment.infra.models :as models]
   [experiment.infra.session :as session]
   [experiment.models.user :as user]
   [experiment.views.common :as common])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers
	handlebars.templates))

;; This file contains templates and other harness for differnet
;; page-level views for the main client application framework

;; ===========================
;; Dashboard

(deftemplate dashboard-header
  [:div
   [:h1 (%strcat (% name) "'s Dashboard")]
   [:div
    [:ul
     [:li (%str "Number of Friends: " (% friends))]
     [:li (%str "Active Experiments: " (% experiments))]]]])


;; ===========================
;; Trial View

;; Take a trial object and render it's header
(deftemplate trial-header
  [:div
   [:h1 (% experiment.name)]])
   

;; Application renders views of data   

;; ===========================
;; Search

(deftemplate discover-filter
  [:div.discover-filter
   [:input {:type "text"
	    :id "discover-filter-input"
	    :value (% query)}]])

;; ===========================
;; Admin
  
(deftemplate admin-main
  [:div [:h1 "This is the admin template"]])
