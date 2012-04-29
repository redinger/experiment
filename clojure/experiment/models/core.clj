(ns experiment.models.core
  (:use experiment.infra.models
        experiment.models.user)
  (:require
   [experiment.models.trial :as trial]
   [experiment.libs.datetime :as dt]
   [experiment.infra.session :as session]))

;; VARIABLE/SYMPTOM [name ref]
;; -----------------------------------------------------------
;;
;; -  has name
;; -  contains Comments

;; TREATMENT [name ref]
;; -----------------------------------------------------------
;;
;; -  has name
;; -  has tags[]
;; -  has averageRating
;; -  server ratings{user: value}
;; -  contains Comments
;;    - op: tag
;;    - op: comment
;;    - op: rate (send to server, update average)

(defmethod valid-model? :treatment [treat]
  (let [{:keys [tags comments warnings]} treat]
    (and (every? (set (keys treat)) [:name :description])
	 (every? #(or (nil? %1) (sequential? %1)) [comments warnings tags]))))
  
(defmethod server->client-hook :treatment [treat]
  (-> treat
      (markdown-convert :description)))

(defmethod public-keys :treatment [treat]
  [:name :tags :description :description-html
   :dynamics :help :reminder :votes :warnings :comments])

(defmethod import-keys :treatment [treat]
  [:description :name :reminder :help :tags])


;; INSTRUMENT [type ref]
;; -----------------------------------------------------------
;;
;; -  has name
;; -  has type
;; -  has variable
;; -  has implementedp -- new instrument objects are requests
;; -  contains Comments

(defmethod public-keys :instrument [treat]
  [:variable :description :description-html :service :tags
   :comments :owner :src])

(defmethod import-keys :instrument [treat]
  [:description :nicknames :tags])

(defn has-tracker-for-inst [user inst]
  (> (count
      (filter #(and (dbref? %) (= (:_id inst) (.getId %)))
              (map :instrument
                   (vals (:trackers user)))))
     0))

(defmethod server->client-hook :instrument [inst]
  (-> inst
      (markdown-convert :description)
      (owner-as-bool :owner :admins (site-admin-refs))
      (assoc :tracked (has-tracker-for-inst (session/current-user) inst))))
      


;; EXPERIMENT
;; -----------------------------------------------------------
;;
;; -  ref Treatment
;; -  ref Instruments[]
;; -  has title
;; -  has instructions
;; -  has tags[]
;; -  submodel Schedule
;; -  submodel Ratings{}
;; -  submodels Comments[]

(defmethod db-reference-params :experiment [model]
  [:treatment :instruments])

;; TRACKER
;; -----------------------------------------------------------
;;
;; - refs User
;; - refs Instrument
;; - has state

(defmethod db-reference-params :tracker [model]
  [:user :instrument])


;; JOURNAL (embedded)
;; -----------------------------------------------------------
;;
;; -  date
;; -  date-str
;; -  content
;; -  sharing

(defmethod db-reference-params :journal [model]
  [:user])

(defmethod public-keys :journal [model]
  [:date :date-str :sharing :short :content :annotation])

(defmethod import-keys :journal [treat]
  [:date :sharing :short :content :annotation])

(defmethod server->client-hook :journal [model]
  (assoc model
    :date-str (dt/as-blog-date (:date model))))


;; COMMENT (embedded)
;; -----------------------------------------------------------
;;
;; -  has upVotes
;; -  has downVotes
;; -  has title
;; -  has content

(defmethod make-annotation :comment [{:keys [text]}]
  (when (> (count text) 5)
    (let [date (dt/now)]
      {:content text
       :username (:username (session/current-user))
       :date (dt/as-utc date)
       :date-str (dt/as-short-string date)})))


