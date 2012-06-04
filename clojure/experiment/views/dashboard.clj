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
;;  (bootstrap-models-json
;;   (concat
;;    (models/fetch-models :treatment)
;;    (models/fetch-models :instrument)
;;    (models/fetch-models :experiment)))
  (bootstrap-user-json)
  )

(defn dashboard-subnav [current]
  {:menu
   [{:tag "overview" :name "Overview" :href "overview"}
;;    {:name "Trials" :href "#"}
    {:tag "timeline" :name "Timeline" :href "timeline"}
    {:tag "events" :name "Events" :href "eventlog"}
;;    {:name "Activity" :href "#"}
    {:tag "journal" :name "Journal" :href "journal"}]
   :active current})

(defpartial dashboard-layout [options]
  (page-frame
   ["Personal Experiments Dashboard"
    :fixed-size 100
    :deps ["views/common", "views/dashboard"]]
   (nav-fixed (:nav (default-nav "dashboard")))
   (subnav-fixed (dashboard-subnav (:subnav options)))
   [:div.container {:style "min-height: 400px;"}
    [:div.tab-content 
     [:div#overview.tab-pane]
     [:div#timeline.tab-pane]
     [:div#eventlog.tab-pane]
     [:div#journal.tab-pane]]]
   (bootstrap-data)        ;; models
   (render-all-templates))) ;; views

(defpage dashboard-dispatch "/dashboard/:subnav" {:as options}
  (dashboard-layout options))

(defpage dashboard-dispatch-args "/dashboard/:subnav/:args*" {:as options}
  (dashboard-layout options))

(defpage dashboard-redir "/dashboard" {:as options}
  (resp/redirect "/dashboard/overview"))
