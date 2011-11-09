(ns experiment.views.app
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

;;
;; Templates
;;

(defn render-template [id template]
  (inline-template
   id template "text/x-jquery-html"))

(defn render-all-templates
  ([]
     (map (fn [[name template]]
	    (render-template name template))
	  (all-templates)))
  ([names]
     (map (fn [[name template]]
	    (render-template name template))
	  (select-keys (all-templates) list))))

;; Dashboard

(deftemplate dashboard-main
  [:div
   [:h1 (%str (% name) "'s Dashboard")]
   [:div
    [:ul
     [:li (%str "Number of Friends: " (% friends))]
     [:li (%str "Active Experiments: " (% experiments))]]]])

;; Search

(deftemplate discover-filter
  [:div.discover-filter
   [:input {:type "text"
	    :id "discover-filter-input"
	    :value (% query)}]])

;; Search views

(deftemplate treatment-list-view
  [:div {:class "result, treatment-list-view"}
   [:h3 (% name)]
   [:p (% description)]])

(deftemplate instrument-list-view
  [:div {:class "result, instrument-list-view"}
   [:h3 (% name)]
   [:p (% description)]])

(deftemplate experiment-list-view
  [:div {:class "result, experiment-list-view"}
   [:h3 (% name)]
   [:p (% description)]])

(deftemplate trial-list-view
  [:div {:class "result, trial-list-view"}
   [:h3 (%with experiment (% name))]
   [:p (% user)]])

(deftemplate comment-short-view
  [:div {:class "comment-short"}
   [:p {:class "comment-text"} (% content)]
   [:p {:class "comment-sig"} (%str "@" (% user) " at [date tbd]")]])


  
;; Admin
  
(deftemplate admin-main
  [:div [:h1 "This is the admin template"]])

;;
;; Ship a single app template for client side
;; application execution
;;

(defpartial app-skeleton []
  [:div#dashboardApp]
  [:div#trialApp]
  [:div#discoverApp]
  [:div#adminApp]
  (render-all-templates))

(defpartial share-skeleton []
  [:div#social])

(defpartial bootstrap-data []
  (let [user (session/current-user)
	username (:username user)]
    [:script {:type "text/javascript"}
     (map #(apply common/bootstrap-collection-expr %)
	  [["window.Instruments" (models/fetch-models :instrument)]
	   ["window.Experiments" (models/fetch-models :experiment)]
	   ["window.MyTrials" (models/fetch-models :trial :where {:user username})]
	   ["window.Treatments" (models/fetch-models :treatment)]])]))

(defpage "/app*" {}
  (common/app-layout
   [["dashboard" "Dashboard"]
    ["trials" "Trials"]
    ["discover" "Search"]
    (when (user/admin?)
      ["admin" "Admin"])]
   (app-skeleton)
   (share-skeleton)
   (bootstrap-data)))


     
