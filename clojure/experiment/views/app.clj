(ns experiment.views.app
  (:require
   [clojure.data.json :as json]
   [noir.response :as resp]
   [experiment.infra.models :as models]
   [experiment.infra.session :as session]
   [experiment.models.user :as user]
   [experiment.models.events :as events]
   [experiment.views.common :as common])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers
	experiment.models.suggestions
	handlebars.templates))

;; This file packages and renders most resources relevant to the
;; client application including client-side templates, data
;; bootstrapping and HTML skeletons.

;; See views/common.clj for the static page template and menu
;; rendering

;;
;; Rendering all templates
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

;;
;; Ship a application skeleton to the client side
;;

(defpartial app-skeleton []
  [:div#dashboardApp]
  [:div#trialApp]
  [:div#searchApp]
  [:div#adminApp]
  [:div#viewTemplates
   (render-all-templates)])

(defpartial share-skeleton []
  [:div#social])

(defpartial bootstrap-data []
  (let [user (session/current-user)
	username (:username user)]
    [:script {:type "text/javascript"}
     (map #(apply common/bootstrap-collection-expr %)
	  [["window.Instruments" (models/fetch-models :instrument)]
	   ["window.Experiments" (models/fetch-models :experiment)]
	   ["window.MyTrials"
	    (models/fetch-models :trial :where {:user username})]
	   ["window.Treatments" (models/fetch-models :treatment)]
	   ["window.MyTrackers"
	    (models/fetch-models :tracker :where {:user.$id (:_id user)})]])
     (str "window.Suggestions.reset("
	  (json/json-str (compute-suggestions))
	  ");")]))

(defpage "/app*" {}
  (common/app-layout
   [["dashboard" "Dashboard"]
    (concat ["trials" "Trials"]
	    (map (fn [model num]
		   [(:_id model) (str "Trial " num)])
		 (models/fetch-models :trial :where {:user (:username (session/current-user))})
		 (range 1 10)))
    ["search" "Search"]
    (when (user/admin?)
      ["admin" "Admin"])]
   (app-skeleton)
   (share-skeleton)
   (bootstrap-data)))


     
