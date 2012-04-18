(ns experiment.views.settings
  (:use
   noir.core
   hiccup.core
   hiccup.page-helpers
   experiment.views.menu
   handlebars.templates)
  (:require
   [noir.response :as resp]
   [experiment.infra.services :as services]
   [experiment.infra.session :as session]
   [experiment.views.common :as common]))


(defn settings-subnav [active]
  {:menu
   [{:tag "preferences" :name "Preferences" :href "preferences"}
    {:tag "services" :name "Services" :href "services"}
    {:tag "password" :name "Change Password" :href "password"}]
   :active active})

(defn settings-nav [active]
  (assoc (common/default-nav "user")
    :subnav (settings-subnav active)))

(defpage settings-page "/account/:subnav" {:keys [subnav] :as options}
  (common/page-frame
   ["Personal Experiments Account Settings"
    :fixed-size 100
    :deps ["views/settings"]]
   (nav-fixed (:nav (common/default-nav "user")))
   (subnav-fixed (settings-subnav subnav))
   [:div.container {:style "min-height: 400px"}
    [:div#main]]
   (common/bootstrap-user-json)
   (services/render-registry)
   (common/render-all-templates)))

(defpage settings-redir "/account" {}
  (resp/redirect "/account/preferences"))

;; Personal Information (TAB 1)
;; - gender
;; - birthdate
;; - weight
;; - height
;; - state
;; - country
;; - time zone

;; View preferences (TAB 2)
;; - date fmt
;; - units
;; - chart pref
;; - my url

;; Contact options
;; - e-mail on related comments
;; - inhibit reminders

;; Services (TAB 2)
;; - List of services (cell, e-mail, withings, etc)

;; Password (TAB 3)

;; Social Media (TAB 4)
;; - Facebook

;;
;; Service View Templates
;;

(deftemplate services-header-template
  [:div
   [:span.page-header
    [:h1.pull-left "Service Connections"]
    [:span.btn-group.pull-right
     [:a.btn.btn-success.dropdown-toggle {:data-toggle "dropdown" :href "#"}
      "Add Service " [:span.caret]]
     [:ul.dropdown-menu
      (%each services
        [:li [:a.new {:href (% tag)} (% name)]])]]]
   [:div {:style "clear: both;"}]
   [:hr]])

(deftemplate service-template
  [:div.span7.well
   [:span.pull-right
    [:button.btn.btn-mini.btn-danger.del
     {:data-tag (% config.tag)}
     [:i.icon-remove.icon-white]]]
   [:h2 (% config.name)]
   [:p (% config.description)]
   [:div.svcform]])

(deftemplate service-oauth-template
  [:div.span7.well
   [:span.pull-right
    [:button.btn.btn-mini.btn-danger.del
     {:data-tag (% config.tag)}
     [:i.icon-remove.icon-white]]]
   [:h2 (% config.name)]
   [:p (% config.description)]
   (%if oauth
        [:p "Connected as : " (% oauth.name)])
   (%unless oauth
        [:p "Connect to " [:a {:href (% config.url)} (% config.title)]])])
