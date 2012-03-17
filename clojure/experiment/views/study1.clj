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
   [clodown.core :as markdown]
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


;; Authoring Study Page
;; --------------------------

;; ## Utilities

(defn consent-patient! []
  (set-pref! :study1-consented true))

(defn patient-consented? []
  (get-pref :study1-consented))

(defn record-experiment [exp]
  (if (> (count (:id exp)) 0)
    (update-model!
     (assoc (dissoc exp :submit)
       :_id (deserialize-id (:id exp))
       :type "study1-experiment"))
    (create-model!
     (assoc (dissoc exp :submit)
       :type "study1-experiment"
       :owner (:username (session/current-user))
       :date (dt/as-utc (dt/now))))))

(defn get-experiment [id]
  (fetch-model :study1-experiment {:_id (as-oid id)}))

(defn get-experiments
  ([]
     (get-experiments (session/current-user)))
  ([user]
     (let [name (if (string? user) user (:username user))]
       (fetch-models :study1-experiment {:owner name}))))
     
(defn study1-complete?
  ([]
     (>= (count (get-experiments)) 2))
  ([user]
     (>= (count (get-experiments user)) 2)))


(defn study1-nav [current]
  (let [user (session/current-user)
        consented? (get-pref :study1-consented)]
    (merge (common/default-nav "Research")
           {:subnav
            {:menu
             (concat
              [{:name "Overview" :href "/study1"}
               {:name "Study Protocol" :href "/study1/doc/study1-protocol"}
               {:name "Consent" :href "/study1/consent"}
               {:name "Online Q&A" :href "/study1/discuss"}]
              (when consented?
                [{:name "Introduction" :href "/study1/doc/study1-background"}
                 {:name "Examples" :href "/study1/doc/study1-example"}
                 {:name "Author Study" :href "/study1/author"}]))
             :active current}})))

;; ## Study home page

(defpartial render-study-page []
  (common/layout
   "Authoring Research Study"
   (study1-nav "Overview")
   [:div.container
    [:div.span8
     [:div.page-header
      [:h1 "Welcome to the Authoring study"]
      [:br]
      [:p "This research study seeks to understand how people
          react to and think about self-experimentation
          and the questions and concerns that arise when they
          try to design their own experiments."]

      [:p "Please read the following documents.  If you want to
         participate in the study, " [:a.register-button {:href "#"} "register"]
         " for an account on this site.  This will
         enable you to login and then to consent to the study (step 2 under Procedures)."]
      (when (session/logged-in?)
        [:p "To withdraw or have your account deleted, please send e-mail eslick@media.mit.edu"])]
     [:h2 "Procedures"]
     [:ol
      [:li (link-to "/study1/doc/study1-protocol" "Review the Study Protocol")]
      (if (session/logged-in?)
        (if (not (patient-consented?))
          [:li (link-to "/study1/consent" "Register for the study")]
          (list [:li "Succesfully Registered"]
                [:li (link-to "/study1/doc/study1-background" "Introduction to Single-Subject Experimentation")]
                [:li (link-to "/study1/doc/study1-example" "Example Experiments")]
                ))
        [:li [:a.login-button {:href "#loginModal"} "Login"] " or " [:a.register-button {:href "#regModal"} "Register" " to get an account."]])]
     (if (patient-consented?)
       (list [:h2 "Actions"]
             [:p "After you have reviewed the preparatory materials above, you need to choose a treatment and create your studies.  You can start working on an experiment at any time.  If you save the experiment, it will show up on this overview page and you can click on it to review or make changes at any time until you finish the study."]
             [:ul
              [:li (link-to "/study1/doc/study1-suggestions" "Choose Treatments to Research")]
              [:li (link-to "/study1/author" "Author an Experiments")]
              [:li "If you need help, go to our " (link-to "/study1/discuss" "Online Q&A") " page"]
              (when (study1-complete?)
                [:li "When you are done with two or more experiments, please take the " (link-to "http://qualtrics.com" "Exit Survey")])])
       (list [:h3 "Actions"]
             [:ul
              [:li (link-to "/study1/discuss" "If you need help, go to our " "Online Q&A")]]))
     (when (and (patient-consented?) (not (empty? (get-experiments))))
       (list [:h3 "Your Experiments"]
             [:ul
              (map (fn [exp]
                     [:li (link-to (format "/study1/author?id=%s" (:_id exp))
                                   (:name exp))])
                   (get-experiments (session/current-user)))]))]]))


(defpage "/study1" {}
  (render-study-page))

;; Information pages

(defpage "/study1/doc/:name" {name :name}
  (let [article (get-article name)]
    (common/layout
     (str "Reading: " (:title article))
     (study1-nav (case name
                   "study1-protocol" "Study Protocol"
                   "study1-background" "Introduction"
                   "study1-example" "Examples"
                   "study1-suggestions" "Suggestions"
                   true "Author"))
     [:div.container
      [:div.span8
       (if article
         (list (when (is-admin?)
                 [:a.admin-link {:href (format "/article/edit/%s" name)}
                  "Edit Article"])
               [:div.page-header
                [:h1 (:title article)]]
               (:html article))
         [:div.page-header
          [:h1 "No Article named '" name "' found"]])]])))

(defpartial render-consent-form []
  (form-to [:post "/study1/consent"]
           [:div.form-actions
            [:button {:type "submit" :class "btn-large btn-success"}
             "YES, I consent to this research"]
            [:button {:type "submit" :class "btn-large btn-danger"}
             "NO, I do not consent"]]))

(defpartial render-consent []
  (let [consent (get-article "study1-consent")]
    [:div.container
     [:div.span8
      [:div.page-header
       [:h1 (:title consent)]]
      (:html consent)
      (if (session/logged-in?)
        (if (patient-consented?)
          [:h2 "You have consented to this study"]
          (render-consent-form))
        [:h2 "You must register and login to consent to this study"])]]))
     
(defpage "/study1/consent" {}
  (common/layout
   "Authoring Study Consent"
   (study1-nav "Consent")
   (render-consent)))

(defpage [:post "/study1/consent"] {}
  (let [req req/*request*]
    (consent-patient!)
    (resp/redirect "/study1")))

;; Discussion page

(defpage "/study1/discuss" {}
  (common/layout
   "Authoring Study Discussion"
   (study1-nav "Online Q&A")
   [:div.container
    [:div.page-header
     [:h1 "Authoring Study Q&A"]]
    [:div.span8
     (discuss/discussions "study1" "/study1/discuss")]]))

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
  (common/layout
   "Authored Experiments"
   (study1-nav "Author")
   [:div.container
    [:h1 "My Submitted Experiments"]
    [:p "It is perfectly acceptable during the duration of this experiment (through early March 2012) to revisit experiments you have submitted and make changes to them"]
    [:ul (map (fn [exp] [:li [:a {:href (format "/study1/author?id=%s" (:_id exp))}
                              (:name exp)]])
              (get-experiments (session/current-user)))]]))

;; Study authoring page

(defn form-control-group [id name control & [help]]
  [:div.control-group
   (label {:class "control-label"} id name)
   [:div.controls
    (when help [:p.help-block [:i help]])
    control]])

(defpartial experiment-form [experiment]
  (form-to [:post "/study1/author"]
           [:fieldset
            (form-control-group "name" "Name"
                                (text-field {:class "input-xlarge"}
                                            "name" (:name experiment))
                                "Give your experiment a meaningful name")
            (form-control-group "treatment" "Treatment"
                                (text-area {:class "input-xxlarge" :rows 10}
                                           "treatment" (:treatment experiment))
                                "Describe the treatment in as much detail as you can so that someone could reproduce all the important aspects of it")
            (form-control-group "outcome" "Outcome"
                                (text-area {:class "input-xxlarge" :rows 3}
                                           "outcome" (:outcome experiment))
                                "What is the primary outcome you are interested in and how would you measure it?")
            (form-control-group "measures" "Other Measures"
                                (text-area {:class "input-xxlarge" :rows 6}
                                           "measures" (:measures experiment))
                                "Please list, separated by commas, other measurements you think might be relevant to this experiment")
            (form-control-group "schedule" "Experiment Schedule"
                                (text-area {:class "input-xxlarge" :rows 6}
                                           "schedule" (:schedule experiment))
                                "How long should someone do a treatment?  How many times should they repeat? <br/> How long should the baseline collection period be?  How long does it take for someone to respond to a treatment?")
            (form-control-group "predictors" "Predictors"
                                (text-area {:class "input-xxlarge" :rows 6}
                                           "predictors" (:predictors experiment))
                                "Based on your research, what symptoms or patient history (if any) is likely to be associated with a successful response to a treatment. <br/>  You can specify this as formally or as causally as you want.")
            (form-control-group "notes" "Notes/Comments"
                                (text-area {:class "input-xxlarge" :rows 6}
                                           "notes" (:notes experiment)))
            (hidden-field "id" (str (:_id experiment)))
            [:div.form-actions
             [:button.btn.btn-primary {:type "submit" :name "submit"} "Save Experiment"]
             [:button.btn {:type "submit" :name "cancel"} "Cancel"]]]))


(defpartial experiment-thank-you [name]
  [:h1 "Submission Successful"]
  [:p "Thank you for submitting the experiment '" name
   "'.  We encourage you to research more possible treatments and provide additional submissions."])

(defpage "/study1/author" {:as options}
  (common/layout
   "Author an Experiment"
   (study1-nav "Author")
   [:div.container
    [:div.study1-author
     [:div.page-header
      [:h1 "Author an Experiment"]
      [:br]
      [:p "Here we ask you to describe all the essential elements of an experiment designed to test a treatment, or a collection of simultaneous treatments.  You can read the introductory material, look at an example experiment, and review the questions on the Q&A page for more information.  The <a href=\"/study1/doc/study1-suggestions\">treatment suggestion page</a> gives you some starting points for treatments and ways to find information about them"]
      [:p "There are no wrong answers here, if you aren't sure about something and can't get what you want from the Q&A section, then write down what is hard or confusing.  The goal is to learn how you react and think about experimentation so we can help make the process easier."]]
     (experiment-form
      (if-let [id (:id options)]
        (get-experiment id)
        {}))]]))

(defpage author-view [:get "/study1/author-view"] {:as options}
  (let [exp (get-experiment (:id options))]
    (common/layout
     "View a Study"
     (study1-nav "")
     [:div.span8
      [:div.study1-author
       (if (not exp)
         [:h1 "Experiment not found"]
         (list [:h2 "Example: " (:name exp)]
               [:dl.dl-horizontal
                [:dt "Treatment"]
                [:dd (markdown/md (:treatment exp))]
                [:dt "Outcome"]
                [:dd (markdown/md (:outcome exp))]
                [:dt "Measures"]
                [:dd (markdown/md (:measures exp))]
                [:dt "Schedule"]
                [:dd (markdown/md (:schedule exp))]
                [:dt "Predictors"]
                [:dd (markdown/md (:predictors exp))]
                [:dt "Notes"]
                [:dd (markdown/md (:notes exp))]]))]])))
   
(defn experiment-valid? [spec]
  true)

(defpage [:post "/study1/author"] {:as spec}
  (common/layout
   "Thank you"
   (study1-nav "Author")
   [:div.container
    (if (and (:submit spec) (experiment-valid? spec))
      (do
        (record-experiment spec)
        (experiment-thank-you (:name spec)))
      (render "/study1/author"))]))

                        

