(ns experiment.views.dashboard
  (:require
   [clojure.data.json :as json]
   [noir.response :as resp]
   [experiment.infra.models :as models]
   [experiment.infra.session :as session]
   [experiment.models.user :as user]
   [experiment.models.profile]
   [experiment.models.events :as events]
   [experiment.views.google :as google])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers
	experiment.models.suggestions
        experiment.views.common
        experiment.views.menu
	handlebars.templates))

;; ---------------------------------
;; Dashboard 
;; ---------------------------------
;;
;; This file packages and renders most resources relevant to the
;; client application including client-side templates, data
;; bootstrapping and HTML skeletons.

;; See views/common.clj for the static page template and menu
;; rendering

(defn current-trials []
  (:trials (session/current-user)))

(defn menu-content []
  [["dashboard" "Dashboard"]
   (concat ["trials" "Trials"]
           (map (fn [model num]
                  [(:_id model) (str "Trial " num)])
                (current-trials)
                (range 1 10)))
   ["search" "Search"]
   (when (user/is-admin?)
     ["admin" "Admin"])])

(defpartial nav-layout []
  [:div#nav-pane.left-side-bar
   [:hr]
   [:div#main-menu.main-menu nil]
;;    (render-menu (menu-content))]
   [:div.nav-footer
    (image "/img/c3ntheme_logo.png" "C3N Logo")
    [:br]
    [:div {:style "text-align: center"}
     (link-to "/article/terms" "Terms of Use")
     "&nbsp; | &nbsp;"
     (link-to "/article/privacy" "Privacy")
     "&nbsp;"]]])

(defpartial bootstrap-data []
  (let [user (session/current-user)
	username (:username user)]
    [:script {:type "text/javascript"}
     (map #(apply bootstrap-collection-expr %)
	  [["window.ExApp.Instruments" (models/fetch-models :instrument)]
	   ["window.ExApp.Experiments" (models/fetch-models :experiment)]
	   ["window.ExApp.MyTrials"
	    (models/fetch-models :trial {:user username})]
	   ["window.ExApp.Treatments" (models/fetch-models :treatment)]
	   ["window.ExApp.MyTrackers"
	    (models/fetch-models :tracker {:user.$id (:_id user)}
				 :only [:user :instrument :type :state])]
           ["window.ExApp.Users"
            (models/fetch-models :user :only [:username :type])]])
     (str "window.ExApp.Suggestions.reset("
	  (json/json-str (compute-suggestions))
	  ");")]))

(defpartial dashboard-layout []
  (page-frame
   ["Personal Experiments Dashboard" 80]
   (nav-fixed (:nav (default-nav "Dashboard")))
   (subnav-fixed (:subnav (default-nav "Dashboard")))
   [:div.container
;;     (app-pane)
;;     (nav-layout)
    [:div.page-header
     [:h1 "Your Personal Experiment Dashboard (COMING SOON)"]]
    [:div.hidden
      (render-all-templates)
      (include-js "/js/app.js")
      (send-user)
      (bootstrap-data)]]))

(defpage "/dashboard*" {}
  (dashboard-layout))


(defpage "/explore*" {}
  (page-frame
   ["Explore Experiments"]
   (nav-fixed (:nav (default-nav "Explore")))
   [:div.container
    [:div.page-header
     [:h1 "Browse and Search Experiments (COMING SOON)"]]]))
  