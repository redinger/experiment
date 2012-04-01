(ns experiment.views.dashboard
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


;; ---------------------------------
;; Dashboard 
;; ---------------------------------
;;
;; This file packages and renders most resources relevant to the
;; client application including client-side templates, data
;; bootstrapping and HTML skeletons.

;; See views/common.clj for the static page template and menu
;; rendering

(defn menu-content []
  [["dashboard" "Dashboard"]
   (concat ["trials" "Trials"]
           (map (fn [model num]
                  [(:_id model) (str "Trial " num)])
                (user/trials)
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
	   ["window.ExApp.Treatments" (models/fetch-models :treatment)]
           ["window.ExApp.Users"
            (models/fetch-models :user :only [:username :type])]])
     (str "window.ExApp.Suggestions.reset("
	  (json/generate-string (compute-suggestions))
	  ");")]))

(defn dashboard-subnav [current]
  {:menu
   [{:tag "overview" :name "Overview" :href "/dashboard/overview"}
;;    {:name "Trials" :href "#"}
    {:tag "timeline" :name "Timeline" :href "/dashboard/timeline"}
    {:tag "events" :name "Events" :href "/dashboard/events"}
;;    {:name "Activity" :href "#"}
    {:tag "journal" :name "Journal" :href "/dashboard/journal"}]
   :active current})

(defpartial dashboard-layout [options]
  (page-frame
   ["Personal Experiments Dashboard"
    :fixed-size 80
    :deps ["views/dialog", "views/dashboard"]]
   (nav-fixed (:nav (default-nav "dashboard")))
   (subnav-fixed (dashboard-subnav (:subnav options)))
   [:div.container
    [:br]
    [:div.row
     [:div.span12
      (trials/trial-summary
       (session/current-user)
       options)]]
    [:hr]
    [:div.row
     [:div.span4 {:style "height:250px;"}
      "Calendar"]
     [:div.span4 {:style "height:250px;"}
      "Trackers"]
     [:div.span4 {:style "height:250px;"}
      "Feed"]]]
   [:div.hidden
    (render-all-templates)
;;    (send-user)
;;    (bootstrap-data)
    ]))

(defpage dashboard-dispatch "/dashboard/:subnav" {:as options}
  (dashboard-layout options))

(defpage dashboard-redir "/dashboard" {:as options}
  (resp/redirect "/dashboard/overview"))

(defpage "/explore*" {:as options}
  (page-frame
   ["Explore Experiments"]
   (nav-fixed (:nav (default-nav "explore")))
   [:div.container
    [:div.page-header
     [:h1 "Browse and Search Experiments (COMING SOON)"]]]))
  