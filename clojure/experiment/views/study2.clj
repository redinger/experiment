(ns experiment.views.study2
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers
	experiment.infra.models
        experiment.models.article
        experiment.views.discuss)
  (:require
   [experiment.models.user :as user]
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


;; Aggregating Self-Experiments Study Page
;; -----------------------------------------

;; ## Utilities

(defn consent-patient! []
  (user/set-pref! :study2-consented true))

(defn patient-consented? []
  (user/get-pref :study2-consented))

(defn record-experiment [exp]
  (if (> (count (:id exp)) 0)
    (update-model!
     (assoc (dissoc exp :submit)
       :_id (deserialize-id (:id exp))
       :type "study2-experiment"))
    (create-model!
     (assoc (dissoc exp :submit)
       :type "study2-experiment"
       :owner (:username (session/current-user))
       :date (dt/as-utc (dt/now))))))

(defn get-experiment [id]
  (fetch-model :study2-experiment {:_id (as-oid id)}))

(defn get-experiments
  ([]
     (get-experiments (session/current-user)))
  ([user]
     (let [name (if (string? user) user (:username user))]
       (fetch-models :study2-experiment {:owner name}))))
     
(defn study2-complete?
  ([]
     (>= (count (get-experiments)) 2))
  ([user]
     (>= (count (get-experiments user)) 2)))


(defn study2-nav [current]
  (let [user (session/current-user)
        consented? (user/get-pref :study2-consented)]
    (merge (common/default-nav "research")
           {:subnav
            {:menu
             (concat
              [{:name "Overview" :href "/study2"}
               {:name "Study Protocol" :href "/study2/doc/study2-protocol"}
               {:name "Consent" :href "/study2/consent"}
               {:name "Online Q&A" :href "/study2/discuss"}]
              (when consented?
                [{:name "Introduction" :href "/study2/doc/study2-background"}
                 {:name "Examples" :href "/study2/doc/study2-example"}
                 {:name "Author Study" :href "/study2/author"}]))
             :active current}})))

;; ## Study home page

(defpartial render-study-page []
  (common/layout
   ["Aggregating Self-Experiments Study"
    (study2-nav "Overview")
    :deps ["views/home"]]
   [:div.container
    [:div.span8
     [:div.page-header
      [:h1 "Self-Experiments for Psoriasis"]
      [:br]
      [:p "This page introduces and guides you through one of two different studies
           that will help you determine whether a specific treatment is helpful for you.
           Participating in this study will help us understand more about these treatments,
           and more about how this site can be improved to support more studies like these."]
      [:p "If you would like to participate, we will ask you to read some introductory material,
           consent to the study"]
      [:p "We are not asking you to engage in any experimentation, but simply to write down how you think it should be done.  The best result for us is that you don't get everyong wrong nor everything right, but are somewhere in the middle. We're looking to understand what is easy and what is hard so we can build the best tools for non-specialists."]
      [:p (when (not (session/logged-in?))
            "To participate, please " [:a.register-button {:href "#"} "register"] " for an account, login and then follow the procedures below.  ") "As you proceed through consenting to the study, the procedure list will be updated." ]
      (when (session/logged-in?)
        [:p "To withdraw or have your account deleted, please send e-mail eslick@media.mit.edu"])
      [:p "If at any time you want help, go to our " (link-to "/study2/discuss" "Online Q&A") " page and ask your question."]]
      
     [:h2 "Procedures"]
     [:ol
      (if (session/logged-in?)
        (if (not (patient-consented?))
          (list [:li (link-to "/study2/doc/study2-protocol" "Read: The Study Protocol")]
                [:li (link-to "/study2/consent" "Consent to the study")])
          (list [:li "Read: " (link-to "/study2/doc/study2-background" "Introduction to Single-Subject Experimentation")]
                [:li "Read: " (link-to "/study2/doc/study2-example" "Example Experiments")]
                [:li "Read: " (link-to "/study2/doc/study2-suggestions" "Treatments to Research")]
                [:li "Do: " (link-to "/study2/author" "Author Experiments")]
                [:li "Do: take the " (link-to "https://atrial.qualtrics.com/SE/?SID=SV_3l3Ix9bjgCxYpj6" "Exit Survey") " (we request you complete at least 2 studies, but if you have significant problems, please fill out the survey anyway)"]))
        (list [:li "Preview the " (link-to "/study2/consent" "Study Consent Form")]
              [:li [:a.login-button {:href "#loginModal"} "Login"] " or " [:a.register-button {:href "#regModal"} "Register" " to get an account."]]))]
     (when (and (patient-consented?) (not (empty? (get-experiments))))
       (list [:h2 "Your Experiments"]
             [:ul
              (map (fn [exp]
                     [:li (link-to (format "/study2/author?id=%s" (:_id exp))
                                   (:name exp))])
                   (get-experiments (session/current-user)))]))]]))


(defpage "/study2" {}
  (render-study-page))

;; Information pages

(defpage "/study2/doc/:name" {name :name}
  (let [article (get-article name)]
    (common/layout
     [(str "Reading: " (:title article))
      (study2-nav (case name
                    "study2-protocol" "Study Protocol"
                    "study2-background" "Introduction"
                    "study2-example" "Examples"
                    "study2-suggestions" "Suggestions"
                    true "Author"))]
     [:div.container
      [:div.span8
       (if article
         (list (when (user/is-admin?)
                 [:a.admin-link {:href (format "/article/edit/%s" name)}
                  "Edit Article"])
               [:div.page-header
                [:h1 (:title article)]]
               (:html article))
         [:div.page-header
          [:h1 "No Article named '" name "' found"]])]])))

(defpartial render-consent-form []
  (form-to [:post "/study2/consent"]
           [:div.form-actions
            [:button {:type "submit" :class "btn-large btn-success"}
             "YES, I consent to this research"]
            [:button {:type "submit" :class "btn-large btn-danger"}
             "NO, I do not consent"]]))

(defpartial render-consent []
  (let [consent (get-article "study2-consent")]
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
     
(defpage "/study2/consent" {}
  (common/layout
   ["Self-Experiments Consent"
    (study2-nav "Consent")]
   (render-consent)))

(defpage [:post "/study2/consent"] {}
  (let [req req/*request*]
    (consent-patient!)
    (resp/redirect "/study2/select")))

(defpartial render-select-page []
  [:div.container
   [:h1 "Select Page"]])

(defpage [:get "/study2/select"] {}
  (common/layout
   ["Select your Experiment"
    (study2-nav "Consent")]
   (render-select-page)))

;; Discussion page

(defpage "/study2/discuss" {}
  (common/layout
   ["Aggregating Self-Experiments Discussion"
    (study2-nav "Online Q&A")]
   [:div.container
    [:div.page-header
     [:h1 "Aggregating Self-Experiments Study Q&A"]]
    [:div.span8
     (discuss/discussions "study2" "/study2/discuss")]]))

(defpage [:post "/study2/discuss"]
  {pid :id text :text :as data}
  (if (comment/valid? text)
    (do
      (comment/comment! "study2" pid text)
      (render "/study2/discuss"))
    (do
      (discuss/with-submission data
        (render "/study2/discuss" data)))))


                        

