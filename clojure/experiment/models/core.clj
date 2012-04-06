(ns experiment.models.core
  (:use experiment.infra.models)
  (:require
   [experiment.models.trial :as trial]
   [experiment.libs.datetime :as dt]
   [experiment.infra.session :as session]))

;; -----------------------------------------------------------
;; VARIABLE/SYMPTOM [name ref]
;;  has name
;;  contains Comments

;; -----------------------------------------------------------
;; TREATMENT [name ref]
;;  has name
;;  has tags[]
;;  has averageRating
;;  server ratings{user: value}
;;  contains Comments
;;  - op: tag
;;  - op: comment
;;  - op: rate (send to server, update average)

(defmethod valid-model? :treatment [treat]
  (let [{:keys [tags comments warnings]} treat]
    (and (every? (set (keys treat)) [:name :description])
	 (every? #(or (nil? %1) (sequential? %1)) [comments warnings tags]))))
  
;;(defmethod public-keys :treatment [treat]
;;  [:_id :type
;;   :name :tags :description :dynamics
;;   :help :reminder :votes :warnings :comments])

;; -----------------------------------------------------------
;; INSTRUMENT [type ref]
;;  has name
;;  has type
;;  has variable
;;  has implementedp -- new instrument objects are requests
;;  contains Comments


;; -----------------------------------------------------------
;; EXPERIMENT
;;  ref Treatment
;;  ref Instruments[]
;;  has title
;;  has instructions
;;  has tags[]
;;  submodel Schedule
;;  submodel Ratings{}
;;  submodels Comments[]

(defmethod db-reference-params :experiment [model]
  [:treatment :instruments])

;; -----------------------------------------------------------
;; TRACKER
;; refs User
;; refs Instrument
;; has state

(defmethod db-reference-params :tracker [model]
  [:user :instrument])


;; -----------------------------------------------------------
;; JOURNAL (embedded)
;;  date
;;  date-str
;;  content
;;  sharing

(defmethod db-reference-params :journal [model]
  [:user])


;; ===========================================================
;; -----------------------------------------------------------
;; COMMENT (embedded)
;;  has upVotes
;;  has downVotes
;;  has title
;;  has content

(defmethod make-annotation :comment [{:keys [text]}]
  (when (> (count text) 5)
    (let [date (dt/now)]
      {:content text
       :username (:username (session/current-user))
       :date (dt/as-utc date)
       :date-str (dt/as-short-string date)})))


