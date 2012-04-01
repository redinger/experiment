(ns experiment.views.settings
  (:use
   noir.core
   hiccup.core
   hiccup.page-helpers)
  (:require
   [noir.response :as resp]
   [experiment.views.common :as common]
   [experiment.infra.session :as session]))


(defn settings-subnav [active]
  {:menu
   [{:tag "personal" :name "Personal Information" :href "/account/personal"}
    {:tag "services" :name "Services" :href "/account/services"}
    {:tag "password" :name "Change Password" :href "/account/password"}]
   :active active})

(defn settings-nav [active]
  (assoc (common/default-nav "user")
    :subnav (settings-subnav active)))

(defpage settings-page "/account/:subnav" {:keys [subnav] :as params}
  (common/layout
   ["Account Settings"
    (settings-nav subnav)
    :deps ["views/settings"]]
   [:div.container
    [:div {:id subnav
           :class (if (= subnav "services")
                    "serviceSettings"
                    "accountSettings")}]]))

(defpage settings-redir "/account" {}
  (resp/redirect "/account/personal"))

;; Personal Information (TAB 1)
;; - gender
;; - birthdate
;; - weight
;; - height
;; - state
;; - country
;; - time zone

;; View options
;; - date fmt
;; - units
;; - chart pref
;; - my url

;; Contact
;; - e-mail on related comments
;; - inhibit reminders


;; Services (TAB 2)
;; - List of services (cell, e-mail, withings, etc)

;; Password (TAB 3)

;; Linked Accounts (TAB 4)
;; - Facebook