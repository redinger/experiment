(ns experiment.models.core
  (:require
   [experiment.models.user :as user])
  (:use experiment.infra.models
	noir.core
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers
	handlebars.templates))

;; ===========================================================
;; VARIABLE/SYMPTOM [name ref]
;;  has name
;;  contains Comments

;; ===========================================================
;; TREATMENT [name ref]
;;  has name
;;  has tags[]
;;  has averageRating
;;  server ratings{user: value}
;;  contains Comments
;;  - op: tag
;;  - op: comment
;;  - op: rate (send to server, update average)

(defmethod valid-model-params? :treatment [treat]
  (let [{:keys [tags comments warnings]} treat]
    (and (every? (set (keys treat)) [:name :description])
	 (every? #(or (nil? %1) (sequential? %1)) [comments warnings tags]))))
  
(defmethod client-keys :treatment [treat]
  [:name :tags :description :dynamics
   :help :reminder :votes :warnings :comments])

(deftemplate treatment-list-view
  [:div {:class "result, treatment-list-view"}
   [:h3 (% name)]
   [:p (% description)]])

;; ===========================================================
;; INSTRUMENT [type ref]
;;  has name
;;  has type
;;  refs Variable.name
;;  has implementedp -- new instrument objects are requests
;;  contains Comments

(deftemplate instrument-list-view
  [:div {:class "result, instrument-list-view"}
   [:h3 (% name)]
   [:p (% description)]])

;; ===========================================================
;; EXPERIMENT
;;  refs User.username 
;;  has title
;;  has instructions
;;  contains Schedule
;;  contains TreatmentRefs[]
;;  ref Instrument[]
;;  has tags[]
;;  contains Ratings{}
;;  contains Comments[]
;;  - op: tag
;;  - op: comment
;;  - op: rate (send to server, update average)

(defmethod db-reference-params :experiment [model]
  [:instruments])

(deftemplate experiment-list-view
  [:div {:class "result, experiment-list-view"}
   [:h3 (% name)]
   [:p (% description)]])

;; ===========================================================
;; TRIAL
;;  refs User
;;  refs Experiment
;;  has outcome #[notstart, abandon, success, fail, uncertain]

(defmethod db-reference-params :trial [model]
  [:experiment])

(defmethod client-keys :trial [model]
  [:duration :experiment :sms? :user])

(defmethod export-hook "trial" [model]
  (assoc model
    :stats {:elapsed 21
	    :remaining 7
	    :intervals 1}))

(deftemplate trial-list-view
  [:div {:class "result, trial-list-view"}
   [:h3 (%with experiment (% title))]
   [:p (%with stats
	      (%str "Run for " (% elapsed) " days with " (% remaining) " remaining"))]])

(deftemplate trial-header
  [:div {:class "trial-header"}
   [:h1 (%strcat "Trial of '" (% experiment.title) "'")]])
   

;; ===========================================================
;; JOURNAL (embedded)
;;  date
;;  content

(deftemplate journal-header
  [:div {:class "journal-list"}])

(deftemplate journal-entry
  [:div {:class "journal-entry"}
   [:h3 {:class "date-header"} (% date)]
   [:p (% content)]])
    

;; ===========================================================
;; COMMENT (embedded)
;;  has upVotes
;;  has downVotes
;;  has title
;;  has content

(deftemplate comment-short-view
  [:div {:class "comment-short"}
   [:p {:class "comment-text"} (% content)]
   [:p {:class "comment-sig"} (%str "@" (% user) " at [date tbd]")]])

;; ===========================================================
;; SCHEDULE