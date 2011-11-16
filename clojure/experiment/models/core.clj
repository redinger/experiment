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
  [:div {:class "result treatment-list-view"}
   [:h3 [:b "Treatment:"] (% name)]
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
  [:div {:class "result instrument-list-view"}
   [:h3 [:b "Instrument"] " for " (% variable) " via " (% src) " entry"]
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
  [:div {:class "result experiment-list-view"}
   [:h3 [:b "Experiment: "] (% title)]])

(deftemplate experiment-view
  [:div.experiment-view
   [:h1.exp-title
      [:span "20 trials have been run, 5 are active"]]
   [:button.run {:type "button"} "Run Experiment"]
   [:div.instruments
    [:h2 "Instruments used"]
    [:ul 
     (%each instruments
	    [:li 
	     [:div.inst-name (% variable)]
	     [:div.inst-description (% description)]
	     [:div.inst-src (% src)]])]]])

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
  [:div {:class "result trial-list-view"}
   [:h3 (%with experiment (% title))]
   [:p (%with stats
	 (%str "Run for " (% elapsed) " days with " (% remaining) " remaining"))]])

(deftemplate trial-view-header
  [:div.trial-header
   [:h1 (%strcat "Trial of '" (% experiment.title) "'")]
   [:div.trial-stats
    [:p "Started: " (% start-str)]
    (%unless donep
	 [:p "Current status: " (% status)])
    (%if donep
	 [:p "Ended: " (% end-str)])]
   [:div.trial-actions
    [:button.pause {:type "button"} "Pause"] " | "
    [:button.stop  {:type "button"} "Stop"] " | "
    [:button.complete {:type "button"} "Complete"]]])
    
(deftemplate trial-table
  [:div.trial-table
   [:h2.trial-table-header]
   [:ul.trial-table-list
    (%each trials
	   [:li.trial-table-list-entry
	    [:span.trial-title (% experiment.title)]
	    [:p (%with stats
		       (%str "Run for " (% elapsed) " days with " (% remaining) " remaining"))]])]])

;; ===========================================================
;; JOURNAL (embedded)
;;  date
;;  date-str
;;  content
;;  sharing

(defmethod make-annotation :journal [{:keys [text]}]
  (when (> (count text) 5)
    (let [date (dt/now)]
      {:content text
       :date (dt/as-utc date)
       :date-str (dt/as-short-string date)})))

(deftemplate journal-viewer
  [:div.journal
   [:div.paging
    [:span "Page " (% page) " of " (% total)]
    [:button.prev {:type "button"} "Prev"]
     "&nbsp; | &nbsp;"
    [:button.next {:type "button"} "Next"]]
   [:h2 "Journal"
    (%if type (%strcat " for " (% type)))]
   [:hr]
   (%each entries
    [:div.journal-entry
     [:h3.date-header "Recorded at " (% date-str)]
     [:span.sharing (% sharing)]
     [:p (% content)]])
   [:div.create {:style "display:none"}
    [:button.create {:type "button"} "Create new entry"]]
   [:div.edit {:style "display:none"}
    [:textarea {:rows 10 :cols 80}]
    [:button.submit {:type "button"} "Submit"]
    [:button.cancel {:type "button"} "Cancel"]]])
    

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
  [:div.comment-short
   [:p.comment-text (% content)]
   [:p.comment-sig
    (%strcat "@" (% username))
    " at " (% date-str)]])

;; ===========================================================
;; SCHEDULE

;; Feeds

