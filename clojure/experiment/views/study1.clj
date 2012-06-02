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
    (merge (common/default-nav "research")
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
   ["Authoring Research Study"
    (study1-nav "Overview")
    :deps ["views/home"]]
   [:div.container
    [:div.span8
     [:div.page-header
      [:h1 "Welcome to the Authoring study"]
      [:br]
      [:p "This is the home page for a one-time research study about authoring experiments.  We are running this study to better understand how patients think about self-experimentation and figuring out how making changes impacts them.  If you would like to help, we will ask you to read some introductory material, research a specific treatment, and write down your " [:b "best guess"] " as to how an experiment should be constructed so you would have confidence if you ran the experiment you would be confident that it worked."]
      [:p "We are not asking you to engage in any experimentation, but simply to write down how you think it should be done.  The best result for us is that you don't get everyong wrong nor everything right, but are somewhere in the middle. We're looking to understand what is easy and what is hard so we can build the best tools for non-specialists."]
      [:p (when (not (session/logged-in?))
            "To participate, please " [:a.register-button {:href "#"} "register"] " for an account, login and then follow the procedures below.  ") "As you proceed through consenting to the study, the procedure list will be updated." ]
      (when (session/logged-in?)
        [:p "To withdraw or have your account deleted, please send e-mail eslick@media.mit.edu"])
      [:p "If at any time you want help, go to our " (link-to "/study1/discuss" "Online Q&A") " page and ask your question."]]
      
     [:h2 "Procedures"]
     [:ol
      (if (session/logged-in?)
        (if (not (patient-consented?))
          (list [:li (link-to "/study1/doc/study1-protocol" "Read: The Study Protocol")]
                [:li (link-to "/study1/consent" "Consent to the study")])
          (list [:li "Read: " (link-to "/study1/doc/study1-background" "Introduction to Single-Subject Experimentation")]
                [:li "Read: " (link-to "/study1/doc/study1-example" "Example Experiments")]
                [:li "Read: " (link-to "/study1/doc/study1-suggestions" "Treatments to Research")]
                [:li "Do: " (link-to "/study1/author" "Author Experiments")]
                [:li "Do: take the " (link-to "https://atrial.qualtrics.com/SE/?SID=SV_3l3Ix9bjgCxYpj6" "Exit Survey") " (we request you complete at least 2 studies, but if you have significant problems, please fill out the survey anyway)"]))
        (list [:li "Preview the " (link-to "/study1/consent" "Study Consent Form")]
              [:li [:a.login-button {:href "#loginModal"} "Login"] " or " [:a.register-button {:href "#regModal"} "Register" " to get an account."]]))]
     (when (and (patient-consented?) (not (empty? (get-experiments))))
       (list [:h2 "Your Experiments"]
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
     [(str "Reading: " (:title article))
      (study1-nav (case name
                    "study1-protocol" "Study Protocol"
                    "study1-background" "Introduction"
                    "study1-example" "Examples"
                    "study1-suggestions" "Suggestions"
                    true "Author"))]
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
        [:h2
         "You must click on register (top right of page) and then return and login to consent to this study"])]]))
     
(defpage "/study1/consent" {}
  (common/layout
   ["Authoring Study Consent"
    (study1-nav "Consent")]
   (render-consent)))

(defpage [:post "/study1/consent"] {}
  (let [req req/*request*]
    (consent-patient!)
    (resp/redirect "/study1")))

;; Discussion page

(defpage "/study1/discuss" {}
  (common/layout
   ["Authoring Study Discussion"
    (study1-nav "Online Q&A")]
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
   ["Authored Experiments"
    (study1-nav "Author")]
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
   ["Author an Experiment"
    (study1-nav "Author")
    :deps []]
   [:div.container
    [:div.study1-author
     [:div.page-header
      [:h1 "Author an Experiment"]
      [:br]
      [:p "Here we ask you to describe all the essential elements of an experiment designed to test a treatment, or a collection of simultaneous treatments.  You can read the introductory material, look at an example experiment, and review the questions on the Q&A page for more information.  The <a href=\"/study1/doc/study1-suggestions\">treatment suggestion page</a> gives you two specific treatments to choose from and ways to find information about them"]
      [:p "There are no wrong answers here, if you aren't sure about something and can't get what you want from the Q&A section, then write down what is hard or confusing.  The goal is to learn how you react and think about experimentation so we can help make the process easier for non-experts."]]
     (experiment-form
      (if-let [id (:id options)]
        (get-experiment id)
        {}))]]))

(defpage author-view [:get "/study1/author-view"] {:as options}
  (let [exp (get-experiment (:id options))]
    (common/layout
     ["View a Study"
      (study1-nav "")]
     [:div.span8
      [:div.study1-author
       (if (not exp)
         [:h1 "Experiment not found"]
         (list [:h2 "Example: " (:name exp)]
               [:dl.dl-horizontal
                [:dt "Treatment"]
                [:dd (markdown/md (or (:treatment exp) ""))]
                [:dt "Outcome"]
                [:dd (markdown/md (or (:outcome exp) ""))]
                [:dt "Measures"]
                [:dd (markdown/md (or (:measures exp) ""))]
                [:dt "Schedule"]
                [:dd (markdown/md (or (:schedule exp) ""))]
                [:dt "Predictors"]
                [:dd (markdown/md (or (:predictors exp) ""))]
                [:dt "Notes"]
                [:dd (markdown/md (or (:notes exp) ""))]]))]])))
   
(defn experiment-valid? [spec]
  true)

(defpage [:post "/study1/author"] {:as spec}
  (common/layout
   ["Thank you"
    (study1-nav "Author")]
   [:div.container
    (if (and (:submit spec) (experiment-valid? spec))
      (do
        (record-experiment spec)
        (experiment-thank-you (:name spec)))
      (render "/study1/author"))]))

                        

