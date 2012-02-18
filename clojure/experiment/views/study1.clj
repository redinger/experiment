(ns experiment.views.study1
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers
	experiment.infra.models
	experiment.models.user
        experiment.models.article
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
  (if-let [user (session/current-user)]
    [:span.study1-login-status "Logged in as: " (:username user)
     " (" (link-to "/action/logout?target=/study1" "Logout") ") "]
    [:div.study1-login-status
    (form-to [:post "/action/login?target=/study1"]
             (label "username" "Username")
             (text-field "username")
             (label "password" "Password")
             (password-field "password")
             (submit-button "Login")
             "&nbsp"
             [:a {:class "forgot-password" :href ""} "Forgot password"]
             )]))

(defn consent-patient! []
  (set-user-property! :study1-consented true))

(defn patient-consented? []
  (get-user-property :study1-consented))


(defn record-experiment [exp]
  (if (> (count (:id exp)) 0)
    (update-model!
     (assoc exp
       :_id (deserialize-id (:id exp))
       :type "study1-experiment"))
    (create-model!
     (assoc exp
       :type "study1-experiment"
       :owner (:username (session/current-user))
       :date (dt/as-utc (dt/now))))))

(defn get-experiment [id]
  (fetch-model :study1-experiment :where {:_id (as-oid id)}))

(defn get-experiments
  ([]
     (get-experiments (session/current-user)))
  ([user]
     (let [name (if (string? user) user (:username user))]
       (fetch-models :study1-experiment :where {:owner name}))))
     
(defn study1-complete?
  ([]
     (>= (count (get-experiments)) 2))
  ([user]
     (>= (count (get-experiments user)) 2)))


;; Study home page

(defpartial render-home-page []
  (common/simple-layout {:header-menu false}
   [:div.article
    [:h2 "Welcome to the Self-Experiment Authoring study"]
    (inline-login)
    [:p "This research study seeks to understand how people
          react to and think about self-experimentation
          and the questions and concerns that arise when they
          try to design their own experiments."]

    [:p "Please read the following documents.  If you want to
         participate in the study, register for an account.  This will
         enable you to login to the study page and consent to the
         study.  This registration will enable you to login to the
         main personal experiments site when it launches.
         To withdraw or have your account deleted, please e-mail
         eslick@media.mit.edu"]
    [:h3 "Procedures"]
    [:ol
     [:li (link-to "/study1/doc/study1-protocol" "Review the Study Protocol")]
     (if (session/logged-in?)
       (if (not (patient-consented?))
         [:li (link-to "/study1/consent" "Register for the study")]
         (list [:li "Succesfully Registered"]
               [:li (link-to "/study1/doc/study1-background" "Introduction to Single-Subject Experimentation")]
               [:li (link-to "/study1/doc/study1-example" "Example Experiments")]
;;               [:li (link-to "/study1/doc/study1-instruments" "Example Measurements")]
               ))
       [:li "Login or " [:a {:class "register-link" :href "/action/register?target=/study1"}
                         "Register"] " to get an account"])]
    (if (patient-consented?)
      (list [:h3 "Actions"]
            [:ul
             [:li (link-to "/study1/doc/study1-suggestions" "Treatments to Research")]
             [:li (link-to "/study1/author" "Author an Experiment")]
             [:li (link-to "/study1/discuss" "Online Q&A")]
             (when (study1-complete?)
               [:li (link-to "http://qualtrics.com" "Take the Survey")])])
      (list [:h3 "Actions"]
            [:ul
             [:li (link-to "/study1/discuss" "Online Q&A")]]))
    (when (and (patient-consented?) (not (empty? (get-experiments))))
      (list [:h3 "My Experiments"]
            [:ul
             (map (fn [exp]
                    [:li (link-to (format "/study1/author?id=%s" (:_id exp))
                                  (:name exp))])
                  (get-experiments (session/current-user)))]))]))


            

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

(defpage "/study1/doc/:name" {name :name}
  (let [article (get-article name)]
    (common/simple-layout {:header-menu false}
     (if article
       [:div#main
        (link-to "/study1" "Return to Study Page...")
        (when (is-admin?)
          [:a.admin-link {:href (format "/article/edit/%s" name)}
           "Edit Article"])
        [:div.article
         [:h1 (:title article)]
         (:html article)]]
       [:div#main
        [:h1 "No Article named '" name "' found"]]))))

(defpartial render-consent-form []
  (form-to [:post "/study1/consent"]
           (submit-button "YES, I consent to this research")
           (submit-button "NO, I do not consent")))

(defpartial render-consent []
  (let [consent (get-article "study1-consent")]
    [:div#main
     [:div.article
       [:h1 (:title consent)]
       (:html consent)
       (render-consent-form)]]))
     
(defpage "/study1/consent" {}
  (common/simple-layout {:header-menu false}
    (render-consent)))

(defpage [:post "/study1/consent"] {}
  (let [req req/*request*]
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

;; ========================================
;; Experiment Template
;; ========================================

(defpage "/study1/review" {}
  (common/simple-layout {:header-menu false}
    [:h1 "My Submitted Experiments"]
    [:p "It is perfectly acceptable during the duration of this experiment (through early March 2012) to revisit experiments you have submitted and make changes to them"]
    [:ul (map (fn [exp] [:li [:a {:href (format "/study1/author?id=%s" (:_id exp))}
                              (:name exp)]])
              (get-experiments (session/current-user)))]))

;; Study authoring page

(defpartial experiment-form [experiment]
  (form-to [:post "/study1/author"]
           [:div.form-field
            [:span.head (label "name" "Name")
             [:br]
             [:i "Give your experiment a meaningful name"]]
            (text-field "name" (:name experiment))]
           [:div.form-field
            [:span.head (label "treatment" "Treatment")
             [:br]
             [:i "Describe the treatment in as much detail as you can so that someone could reproduce all the important aspects of it"]]
            (text-area "treatment" (:treatment experiment))]
           [:div.form-field
            [:span.head (label "outcome" "Outcome")
             [:br]
             [:i "What is the primary outcome you are interested in and how would you measure it?"]]
            (text-area "outcome" (:outcome experiment))]
           [:div.form-field
            [:span.head (label "measures" "Other_measures")
             [:br]
             [:i "Please list, separated by commas, other measurements you think might be relevant to this experiment"]]
            (text-area "measures" (:measures experiment))]
           [:div.form-field
            [:span.head (label "schedule" "Experiment Schedule")
             [:br]
             [:i "How long should someone do a treatment?  How many times should they repeat?  How long should the baseline collection period be?  How long does it take for someone to respond to a treatment?"]]
            (text-area "schedule" (:schedule experiment))]
           [:div.form-field
            [:span.head (label "predictors" "Predictors")
             [:br]
             [:i "Based on your research, what symptoms or patient history (if any) is likely to be associated with a successful response to a treatment.  You can specify this as formally or as causally as you want."]]
            (text-area "predictors" (:predictors experiment))]
           [:div.form-field
            [:span.head (label "notes" "Notes")
             [:br]
             [:i "Please enter any other notes or comments here."]]
            (text-area "notes" (:notes experiment))]
           (hidden-field "id" (str (:_id experiment)))
           [:input {:type "submit" :name "submit" :value "Save Experiment"}]
           [:input {:type "submit" :name "cancel" :value "Cancel"}]))


(defpartial experiment-thank-you [name]
  [:h1 "Submission Successful"]
  [:p "Thank you for submitting the experiment '" name
   "'.  We encourage you to research more possible treatments and provide additional submissions."]
  [:a {:href "/study1"} "Return to Study Page"])

(defpage "/study1/author" {:as options}
  (common/simple-layout {:header-menu false}
    [:a {:href "/study1"} "Return to Study Page"]
    [:div.study1-author
     [:h1 "Author an Experiment"]
     [:p "Here we ask you to describe all the essential elements of an experiment designed to test a treatment, or a collection of simultaneous treatments.  You can read the introductory material, look at an example experiment, and review the questions on the Q&A page for more information.  The <a href=\"/study1/doc/study1-suggestions\">treatment suggestion page</a> gives you some starting points for treatments and ways to find information about them"]
     [:p "There are no wrong answers here, if you aren't sure about something and can't get what you want from the Q&A section, then write down what is hard or confusing.  The goal is to learn how you react and think about experimentation so we can help make the process easier."]
     (experiment-form
      (if-let [id (:id options)]
        (get-experiment id)
        {}))]))

;; (defpartial author-view [:get "/study1/author-view"] {:as options}
;;   (let [
;;   (common/simple-layout {:header-menu false}
;;     [:a {:href "/study1"} "Go to the Study Page"]
;;     [:div.study1-author
;;      [:h1 "Review Experiment: " 
    
(defn experiment-valid? [spec]
  true)

(defpage [:post "/study1/author"] {:as spec}
  (common/simple-layout {:header-menu false}
    (if (and (:submit spec) (experiment-valid? spec))
      (do
        (record-experiment spec)
        (experiment-thank-you (:name spec)))
      (render "/study1/author"))))

                        

