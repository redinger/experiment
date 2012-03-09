(ns experiment.models.profile
  (:use experiment.infra.models
        noir.core
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers
	handlebars.templates)
  (:require [experiment.libs.datetime :as dt]
            [experiment.infra.session :as session]))

(defn profile-field
  ([name text value]
     [:div.profile-field
      (label name text)
      (text-field name value)]))

(defn profile-password
  ([name text value]
     [:div.profile-field
      (label name text)
      (password-field name value)]))

(defn profile-textarea
  ([name text value]
     [:div.profile-field
      (label name text)
      [:textarea {:name name :id name}
       value]]))

(defn profile-select
  ([name text value list]
     [:div.profile-field
      (label name text)
      (drop-down name list value)]))

(defmacro profile-service [name & body]
  `[:div.profile-service
    [:h3 ~name [:img {:class "open-close-div" :src "img/plus.png"}]]
    ~@body])

(defn timezone-list []
  [["AST -4" "ast"]
   ["PST -5" "pst"]
   ["MST -6" "mst"]
   ["CST -7" "cst"]
   ["EST -8" "est"]])

(deftemplate profile-edit-view
  [:div
   [:div#info.profile-view
    [:h2 "My Information"]
    [:p#username "Username: " (% username)]
    (profile-field :name "Full Name" (% name))
    (profile-textarea :bio "Bio / Highlights" (% bio))
    (profile-field :location "Location" (% location))]
   [:div#settings.profile-view
    [:h2 "View Options"]
    (profile-field :datefmt "Date Format" (% datefmt))]
    (profile-select :timezone "Time Zone" (% timezone) (timezone-list))
   [:div#email.profile-view
    [:h2 "Email Preferences"]
    (check-box "email-update" (% email-update))
    (label "email-update" "Send me Personal Experiment Updates")]
   [:div#services.profile-view
    [:h2 "Data Services"]
    (profile-service
      "SMS Messaging"
      (profile-field "cell" "Cell Number" (% cell)))
    (profile-service
     "RescueTime"
     (profile-field "rt-user" "Account Email" (% rt-user))
     (profile-field "rt-api" "API Key" (% rt-api)))
    (profile-service
     "Zeo Personal Sleep Coach"
     (profile-field "zeo-user" "Account Email" (% zeo-user))
     (profile-password "zeo-pw" "Zeo Password" (% zeo-pw)))
    (profile-service
     "Strava"
     (profile-field "strava-user" "Account Email" (% strava-user))
     (profile-password "strava-pw" "Strava Password" (% strava-pw)))
    (profile-service
     "WiThings" 
     (%if wi-user
          [:p "Your WiThings account is linked"]
          [:a {:href ""} "Link your WiThings Account"]))
    (profile-service
     "IMAP Email" "")
    (profile-service
     "FitBit" "")
    (profile-service
     "Jawbone Up" "")
    ]
   ])

           

;; Internal accessors

(defn get-user-profile [user]
  (:profile user))
    
(defn set-user-profile!
  ([user profile]
     (set-submodel! user :profile profile)))

