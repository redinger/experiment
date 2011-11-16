(ns experiment.models.core
  (:use experiment.infra.models
	noir.core
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers
	handlebars.templates)
  (:require
   [experiment.libs.datetime :as dt]
   [experiment.infra.session :as session]))

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

(deftemplate treatment-view
  [:div {:class "treatment-view"}
   [:h1 (% name)]])

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

(deftemplate instrument-short-table
  [:div {:class "instrument-short-table"}
   [:ul
    (%each instruments
	   [:li
	    [:a {:href (%strcat "/app/search/instrument/" (% id))}
	     [:span {:class "variable"} (% variable)]
	     [:span {:class "type"} (% src)]]])
    ]])

(deftemplate instrument-view
  [:div {:class "instrument-view"}
   [:h1 (% variable)]
   [:p (% src)]])

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
   [:h3 (% title)]])

(deftemplate experiment-view
  [:div {:class "experiment-view"}
   [:h1 {:class "exp-title"}
    (% title)]
   [:span (%str (% trials) " trials")]
   [:ul 
    (%each instruments
     [:li 
      [:div {:class "inst-name"} (% variable)]
      [:div {:class "inst-description"} (% description)]
      [:div {:class "inst-src"} (% src)]])]])

;; ===========================================================
;; TRIAL
;;  refs User
;;  refs Experiment
;;  has outcome #[notstart, abandon, success, fail, uncertain]

(defmethod db-reference-params :trial [model]
  [:experiment])

(defmethod client-keys :trial [model]
  [:duration :experiment :sms? :user])

(defn human-status [trial]
  ({:active "Active"
    :paused "Paused"
    :abandoned "Abandoned"
    :completed "Completed"}
   (keyword (:status trial))))

(defn trial-done? [trial]
  (when (#{:abandoned :completed} (:status trial)) true))
	     
(defmethod export-hook "trial" [trial]
  (assoc trial
    :stats {:elapsed 21
	    :remaining 7
	    :intervals 1}
    :start-str (dt/as-short-date (:start trial))
    :status (human-status trial)
    :donep (trial-done? trial)
    :end-str (when-let [end (:end trial)] (dt/as-short-string end))))

(deftemplate trial-list-view
  [:div {:class "result, trial-list-view"}
   [:h3 (%with experiment (% title))]
   [:p (%with stats
	 (%str "Run for " (% elapsed) " days with " (% remaining) " remaining"))]])

(deftemplate trial-view-header
  [:div {:class "trial-header"}
   [:h1 (%strcat "Trial of '" (% experiment.title) "'")]
   [:div {:class "trial-stats"}
    [:p "Started: " (% start-str)]
    (%unless donep
	 [:p "Current status: " (% status)])
    (%if donep
	 [:p "Ended: " (% end-str)])]
   [:div {:class "trial-actions"}
    [:a {:class "pause" :href "#"} "Pause"]
    [:a {:class "stop" :href "#"} "Stop"]
    [:a {:class "complete" :href "#"} "Complete"]]])
    
(deftemplate trial-table
  [:div {:class "trial-table"}
   [:h2 {:class "trial-table-header"}]
   [:ul {:class "trial-table-list"}
    (%each trials
	   [:li {:class "trial-table-list-entry"}
	    [:span {:class "trial-title"} (% experiment.title)]
	    [:p (%with stats
		       (%str "Run for " (% elapsed) " days with " (% remaining) " remaining"))]])]])

;; ===========================================================
;; JOURNAL (embedded)
;;  date
;;  content

(deftemplate journal-header
  [:div {:class "journal-list"}])

(deftemplate journal-entry
  [:div {:class "journal-entry"}
   [:h3 {:class "date-header"} "Recorded at " (% date-str)]
   [:span {:class "sharing"} (% sharing)]
   [:p (% content)]])
    

;; ===========================================================
;; COMMENT (embedded)
;;  has upVotes
;;  has downVotes
;;  has title
;;  has content

(defmethod model-collection :comment [model]
  :comments)

(defmethod make-annotation :comment [{:keys [text]}]
  (when (> (count text) 5)
    (let [date (dt/now)]
      {:content text
       :username (:username (session/current-user))
       :date (dt/as-utc date)
       :date-str (dt/as-short-string date)})))

(deftemplate comment-short-view
  [:div {:class "comment-short"}
   [:p {:class "comment-text"} (% content)]
   [:p {:class "comment-sig"}
    (%strcat "@" (% username))
    " at " (% date-str)]])

;; ===========================================================
;; SCHEDULE