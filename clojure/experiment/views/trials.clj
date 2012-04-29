(ns experiment.views.trials
  (:use
   noir.core
   hiccup.core
   hiccup.page-helpers
   hiccup.form-helpers
   experiment.infra.models
   experiment.models.trial
   experiment.views.common
   handlebars.templates)
  (:require
   [clodown.core :as md]
   [noir.response :as resp]
   [experiment.infra.session :as session]
   [experiment.models.user :as user]
   [experiment.models.profile]
   [experiment.views.bootstrap :as bs]))

(def default-options
  {:tn 1} ;; trial number
  )
  
(defn treatment-href [treatment]
  (assert (:_id treatment))
  (str "/explore/treatment/" (:_id treatment)))

(defn trial-empty-summary []
  [:div.page-header
   [:h1 "No Active Trials"]
   [:p [:a {:href "/explore"} "Search for an experiment to run"]]])

(defn trial-controls [status]
  (let [play-class (if (= status :active) "btn disabled" "btn")
        pause-class (if (= status :paused) "btn disabled" "btn")] 
    (list
     (human-status status)
     "&nbsp;"
     [:span.btn-toolbar
      [:span.btn-group
       [:button.btn [:i.icon-play]]
       [:button.btn [:i.icon-pause]]
       [:button.btn [:i.icon-backward]]
       [:button.btn [:i.icon-stop]]]])))

(defn trial-summary-body [user trials options]
  (let [options (if (:tn options) options default-options)
        trial-num (:tn options)
        trial (nth trials (- trial-num 1))
        exp (:experiment trial)
        treat (:treatment exp)]
    [:div.trialSummary.well
;;     [:p "Experiment: " (:title exp)]
     [:div.trialBar
      [:h3
       [:span "Treatment: "
        (bs/popover-link (:name treat)
                         (treatment-href treat)
                         "Treatment Description"
                         (md/mdp (:description treat)))]
       "&nbsp;"
       [:span "Primary Outcome: "
        (:variable (:outcome exp))]
       [:span.pull-right
        (trial-controls (:status trial))]]]
     [:div#qichart {:style "height:250px"}]]))

(defn trial-summary [user options]
  (if-let [trials (user/trials user)]
    (trial-summary-body user
                        (map embed-dbrefs trials)
                        (if (empty? options) default-options options))
    (trial-empty-summary)))
    
(defn trial-complete [user]
  [:div [:p "Foo"]])
