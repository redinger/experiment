(ns experiment.models.events
  (:require
   [experiment.models.user :as user]
   [experiment.infra.session :as session]
   [clj-time.coerce :as coerce])
  (:use experiment.infra.models
	clj-time.core
	clj-time.format
	noir.core
	hiccup.core
	hiccup.page-helpers
	hiccup.form-helpers
	handlebars.templates))


;; 2) Time series
;; 3) Reminders
;; 4) Calendar

;; ==================================
;; Events
;; ==================================

(comment
  ;; Definition of study actions over time
  {:type "schedule"
   }
  
  ;; Data from an API
  {:type "dump"
   :user [] ;; REF
   }
  
  ;; Data produced by instrument (direct or extracted from a report)
  {:type "event"
   :user [] ;; REF
   :instrument [] ;; REF
   :data [] ;; Series of pairs (date as long ms, instrument value)
   }

  {:type "reminder"
   :datetime true
   :instrument true
   :msg true
   :channel true
   :sent? true
   :submit? true
   }

  {:type "report"
   :start_date true
   :end_date true
   :subtype true
   }
  )



;; A study schedule
;; Generates reminders 
;; User actions generate events
;; Background API dumps generate events
;; Events -> reports (for a time range)
;; Events -> Calendar

(defn user-events []
  (let [user (session/current-user)]
    nil))

;; Are events time or periods?
(defn events-for-month []
  )

(defn filter-events
  "Filters events according to inclusive start and end dates"
  [startdate enddate events])


;; ==================================
;; Calendar Views
;; ==================================

(def ^:dynamic *events*)

(defpartial render-first-week [month]
  [:tr
   [:td {:class "padding" :colspan 3}]
   [:td 1]
   [:td {:class "date_has_event"}
    2
    [:div {:class "events"}
     [:ul
      [:li
       [:span {:class "title"} "Record fatigue"]
       [:span {:class "desc"} "Respond to SMS fatigue question"]]
      [:li
       [:span {:class "title"} "QOL Questionnaire"]
       [:span {:class "desc"} "Fill out online QOL Questionnaire"]]]]]
   [:td {:class "today"} 3]
   [:td 4]])

(defpartial render-last-week [month]
  [:div])

(defpartial render-calendar-table [month events]
  (let [start-pad 3
	start-days 4
	end-pad 1
	end-days 6]
    (binding [*events* events]
      [:div {:class "calendar"}
       [:table {:cellspacing 0}
	[:thead
	 [:tr (map (fn [day] [:th day])
		   ["Mon" "Tue" "Wed" "Thu" "Fri" "Sat" "Sun"])]]
	[:tbody
	 (render-first-week month)
	 (render-first-week month)
	 (render-first-week month)
	 (render-first-week month)	    
	 (render-first-week month)]
	[:tfoot 
	 [:tr (map (fn [day] [:th day])
		   ["Mon" "Tue" "Wed" "Thu" "Fri" "Sat" "Sun"])]]]])))

	    
     