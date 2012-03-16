(ns experiment.views.profile
  (:require
   [experiment.views.common :as common]
   [clojure.data.json :as json]
   [experiment.infra.session :as session])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers))

(defpage settings-page "/settings" {:as params}
  (common/layout
   "Account Settings"
   (common/default-nav nil)
   [:div.container
    [:page-header
     [:h1 "My Settings"]]
    [:div.row
     [:div.span6.personal-profile
      [:h2 "Personal Information"]
      [:form.well
       [:input {:type "text" :placeholder "testing"}]]]
     [:div.span6.services-profile
      [:h2 "Services"]]]]))

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