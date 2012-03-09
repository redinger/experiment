(ns experiment.views.calendar
  (:require
   [experiment.models.user :as user]
   [experiment.infra.session :as session]
   [experiment.libs.datetime :as dt]
   [experiment.models.schedule :as sched]
   [experiment.models.events :as events]
   [clj-time.core :as time])
  (:use experiment.infra.models
	noir.core
	hiccup.core
	hiccup.page-helpers
	hiccup.form-helpers
	handlebars.templates))

;; ==================================
;; Calendar View
;; ==================================

(def ^:dynamic *events* {})
(def ^:dynamic *today* nil)

(defn today? [day]
  (= day *today*))

(defpartial render-event [event]
  [:li
   [:span {:class "title"} (:title event)]
   [:span {:class "desc"} (:desc event)]])

(defn events-for-day [day]
  (*events* day))

(defn treatment? [event]
  (:treatment event))

(defpartial render-day [day]
  (let [events (events-for-day day)
	class (clojure.string/join
	       " "
	       [(when (today? day) "today")
		(when events
		  (if (some treatment? events)
		    "treat"
		    "date_has_event"))])]
    [:td {:class class}
     day
     (when events
       [:div {:class "events"}
	[:ul
	 (map render-event events)]])]))
  
(defpartial render-first-week [padding range]
  [:tr
   (when (> padding 0) [:td {:class "padding" :colspan padding}])
   (map render-day range)])

(defpartial render-week [[padding range]]
  [:tr
   (map render-day range)
   (when (> padding 0) [:td {:class "padding" :colspan padding}])])

(defn- events-daily-map [month events]
  ;; NOTE: TODO
  (zipmap (range 1 25 3)
	  (repeat [{:title "Record fatigue"
		    :spec "Respond to SMS fatigue question"}
		   {:title "QOL Questionnaire"
		    :desc "Fill out online QOL Questionnaire"}])))

(defn- make-month [month-ref]
  (if month-ref
    (let [[year month] month-ref]
      (org.joda.time.LocalDate. year month 1))
    (.withDayOfMonth (org.joda.time.LocalDate.) 1)))

(defn- month-padding [month]
  (- (.getDayOfWeek month) 1))

(defn- month-end-day [month]
  (.getDayOfMonth
   (-> month
       (.plusMonths 1)
       (.minusDays 1))))

(defn- compute-weeks [start-pad last-day]
  (letfn [(week [start]
		(let [next (+ start 7)]
		  (cond (< last-day start)
			nil
			(< last-day next)
			(cons [(- next last-day) (range start last-day)] (week next))
			true
			(cons [0 (range start (+ start 7))] (week next)))))]
    (week (- 8 start-pad))))
		 

(defpartial render-calendar-table [month events]
  (let [month (make-month month)
	start-pad (month-padding month)
	last-day (month-end-day month)
	weeks (compute-weeks start-pad last-day)]
    (binding [*events* (events-daily-map month events)]
      [:div {:class "calendar"}
       [:table {:cellspacing 0}
	[:thead
	 [:tr (map (fn [day] [:th day])
		   ["Mon" "Tue" "Wed" "Thu" "Fri" "Sat" "Sun"])]]
	[:tbody
	 (cons (render-first-week start-pad (range 1 (- 8 start-pad)))
	       (map render-week weeks))]]])))

;;
;; Calendar Client REST API
;;

(deftemplate small-calendar
  [:div {:id (% id) :class "small-calendar"}])

(defpage trial-calendar [:get "/api/calendar/trial/:id"] {:keys [id year month start]}
  (let [trial (resolve-dbref "trial" id)
	start (or start (dt/as-utc (dt/now)))]
    (try 
      (render-calendar-table (when (and month (not (= month "now")))
			       [(Integer/parseInt year)
				(Integer/parseInt month)
				(Long/parseLong start)])
                             []))))
;;			     (mapcat #(sched/events % (filter (partial events/future-reminder? start)
;;                                                              (events/trial-reminders trial)))
;;      (catch java.lang.Throwable e
;;	"<b>Calendar Render Error</b>"))))))

;;(defpage experiment-calendar [:get "/api/calendar/experiment/:id"] {:keys [id]}
;;  (let [experiment (resolve-dbref "experiment" id)]
;;    (try
;;      (render-calendar-table nil (generate-reminders experiment (dt/now)
;;						     {:reminders? true}))
;;      (catch java.lang.Throwable e
;;	"<b>Calendar Render Error</b>"))))
