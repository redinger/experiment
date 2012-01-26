(ns experiment.views.study1
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers
	experiment.infra.models
        experiment.views.discuss)
  (:require
   [clojure.tools.logging :as log]
   [clojure.data.json :as json]
   [clojure.string :as str]
   [somnium.congomongo :as mongo]
   [noir.response :as resp]
   [noir.request :as req]
   [noir.util.crypt :as crypt]
   [noir.validation :as vali]
   [experiment.infra.session :as session]
   [experiment.views.common :as common]
   [experiment.models.comment :as comment]
   [experiment.views.discuss :as discuss]
   [experiment.infra.auth :as auth]
   [experiment.libs.datetime :as dt]))

;; Utilities

(defpartial inline-login []
  (form-to [:post "/action/login?target=/study1"]
           (label "username" "Username")
           (text-field "username")
           (label "password" "Password")
           (password-field "password")
           (submit-button "Login")))

(defn fetch-user-profile [user]
  (fetch-model :profile :where {:username (:username user)}))

(defn consent-patient! []
  (let [profile (fetch-user-profile (session/current-user))]
    (update-model! (assoc profile :study1-consented true))))

(defn patient-consented? []
  (let [profile (fetch-user-profile (session/current-user))]
    (:study1-consented profile)))

;; Study home page

(defpartial render-home-page []
  (common/simple-layout {:header-menu false}
   [:div
    [:h2 "Welcome to the world of single-subject experimentation"]
    (when (not (session/logged-in?))
      (inline-login))

    [:p "This research study seeks how we can better apply the
         scientific method to exploring the impact of the everyday
         decisions we make about our health."]

    [:p "Please read the following documents.  If you want to
         participate in the study, accept the consent and register for
         an account.  This will enable you to login to the study page,
         perform the tasks we have prepared for you and complete the
         study.  When we launch the site your account will allow you
         to login to the full site."]

    [:h3 "Procedures"]
    [:ol
     (if (session/logged-in?)
       (if (not (patient-consented?))
         [:li (link-to "/study1/consent" "Register for the study")]
         [:li "Registered for the study"])
       [:li "Login or " [:a {:class "register-link" :href "/action/register?target=/study1"}
                         "Register"] " to get an account"])
     [:li (link-to "/study1/doc/study1-protocol" "Read the Study Protocol")]
     [:li (link-to "/study1/doc/study1-background" "Introduction to Single-Subject Experimentation")]]
    (when (patient-consented?)
      (list [:h3 "Actions"]
            [:ul
             [:li (link-to "/study1/discuss" "Online Q&A")]
             [:li (link-to "/study1/doexp/" "Author a Study")]
             [:li (link-to "/action/logout" "Logout")]]))]))

(defpage "/study1" {}
  (render-home-page))

(defpartial render-login-error []
  (common/simple-layout {:header-menu false}
   [:div
    [:h2 "Welcome to the world of single-subject experimentation"]
    [:p "We are sorry, but your login was not recognized.  Email eslick@media.mit.edu for help."]]))

(defpage [:post "/study1"] {:as args}
  (if (auth/login args)
    (resp/redirect "/study1")
    (render-login-error)))

;; Information pages

(defn format-article [text]
  (str/replace text #"\n" "<br>"))

(defn get-article [name]
  (fetch-model :article :where {:name name}))

(defpage "/study1/doc/:name" {name :name}
  (let [article (get-article name)]
    (common/simple-layout {:header-menu false}
     (if article
       [:div#main
        (link-to "/study1" "Return to Study Page...")
        [:h1 (:title article)]
        [:br][:br]
        (format-article (:body article))]
       [:div#main
        [:h1 "No Article named '" name "' found"]]))))

(defpartial render-consent-form []
  (form-to [:post "/study1/consent"]
           (submit-button "Yes, I consent to this research")
           (submit-button "No, I do not consent")))

(defpartial render-consent []
  (let [consent (get-article "study1-consent")]
    [:div#main
     [:h1 (:title consent)]
     [:br] [:br]
     (format-article (:body consent))
     [:br] [:br]
     (render-consent-form)]))
     
(defpage "/study1/consent" {}
  (common/simple-layout {:header-menu false}
    (render-consent)))

(defpage [:post "/study1/consent"] {}
  (let [req req/*request*]
    (println req)
    (consent-patient!)
    (resp/redirect "/study1")))

;; Discussion page

(defpage "/study1/discuss" {}
  (common/simple-layout {:header-menu false}
    [:a {:href "/study1"} "Return to Study Page"]
    (discuss/discussions "study1" "/study1/discuss")))

(defpage [:post "/study1/discuss"]
  {pid :id text :text :as data}
  (if (comment/valid? text)
    (do
      (comment/comment! "study1" pid text)
      (render "/study1/discuss"))
    (do
      (discuss/with-submission data
        (render "/study1/discuss" data)))))

;; Study authoring page




