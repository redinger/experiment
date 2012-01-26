(ns experiment.views.experiment1
  (:require
   [experiment.views.common :as common]
   [experiment.infra.session :as session]
   [experiment.infra.auth :as auth]
   [clojure.data.json :as json]
   [clojure.string :as str]
   [somnium.congomongo :as mongo]
   [noir.response :as resp]
   [noir.util.crypt :as crypt])
  (:use noir.core
	experiment.infra.models
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers))  

(defpartial inline-login []
  (form-to [:post "/exp1"]
           (label "username" "Username")
           (text-field "username")
           (label "password" "Password")
           (password-field "password")
           (submit-button "Login")))
  
(defpartial render-home-page []
  (common/simple-layout
   [:div
    [:h2 "Welcome to the world of single-subject experimentation"]
    (when (not (session/logged-in?))
      (inline-login))
    [:p "This page is the home page to guide you through participating in our
         research study of potential 'Citizen Scientists' who would like to
         apply scientific methodology to exploration of the impact of the
         decisions we make about our health every day."]
    [:p "Please read the following documents and if you want to participate
         in the study, read the consent and register for an account.  This will
         enable you to login to the study page, perform the tasks we have prepared
         for you and complete the study."]
    [:ol
     [:li (link-to "/exp1/doc/exp1-protocol" "Read the Study Protocol")]
     [:li (link-to "/exp1/doc/exp1-intro" "Introduction to Single-Subject Experimentation")]
     [:li (link-to "/exp1/consent" "Read the consent and register")]]]))

(defpage "/exp1" {}
  (render-home-page))


(defpartial render-login-error []
  (common/simple-layout
   [:div
    [:h2 "Welcome to the world of single-subject experimentation"]
    [:p "We are sorry, but your login was not recognized.  Email eslick@media.mit.edu for help."]]))

(defpage [:post "/exp1"] {:as args}
  (if (auth/login args)
    (render-home-page)
    (render-login-error)))


(defn format-article [text]
  (str/replace text #"\n" "<br>"))

(defpage "/exp1/doc/:name" {}
  (let [article (fetch-model :article :where {:name name})]
     (if article
       [:div
	[:h1 (:title article)]
	(if (session/logged-in?)
	  (link-to "/app/dashboard" "Return to Home Page...")
	  (link-to "/" "Return to Home Page..."))
	[:br][:br]
	(format-article (:body article))]
       [:div#main
	[:h1 "No Article named '" name "' found"]])))

    
   
   
  