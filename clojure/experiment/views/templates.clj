(ns experiment.views.templates
  (:use experiment.infra.models
        noir.core
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers
	handlebars.templates))


;;
;; Template Views for various System Objects
;;


;; TRIAL

(deftemplate trial-list-view
  [:div {:class "result trial-list-view"}
   [:h3 (%with experiment (% title))]
   [:p (%with stats
	 (%str "Run for " (% elapsed) " days with " (% remaining) " days remaining"))]])

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
		       (%str "Run for " (% elapsed) " days with " (% remaining) " days remaining"))]])]])


;; TREATMENT

(deftemplate treatment-list-view
  [:div {:class "result treatment-list-view"}
   [:h3 [:b "Treatment:"] (% name)]
   [:p (% description)]])

(deftemplate treatment-view
  [:div {:class "treatment-view"}
   [:h1 (% name)]
   [:p [:b "Description: "] (% description)]
   [:p [:b "Tags: "] (% tags)]])


;; INSTRUMENT

(deftemplate instrument-list-view
  [:div {:class "result instrument-list-view"}
   [:h3 [:b "Instrument"] " for " (% variable) " (" (% svc) " tracking)"]
   [:p (% description)]])

(deftemplate instrument-short-table
  [:div {:class "instrument-short-table"}
   [:ul
    (%each instruments
	   [:li
	    [:a {:href (%strcat "/app/search/instrument/" (% id))}
	     [:span {:class "variable"} (% name)]
	     [:span {:class "type"} (% src)]]])
    ]])

(deftemplate instrument-view
  [:div {:class "instrument-view object-view"}
   [:h1 (% name)]
   [:p [:b "Source: "] (% src)]
   (%if nicknames [:p [:b "Nicknames: "] (% nicknames)])
   [:p [:b "Description: "] (% description)]])


;; EXPERIMENT
   
(deftemplate experiment-list-view
  [:div {:class "result experiment-list-view"}
   [:h3 [:b "Experiment: "] (% name)]])

(deftemplate experiment-view
  [:div.experiment-view
   [:h1.exp-title
      [:span (% name)]]
   [:button.run {:type "button"} "Run Experiment"]
   [:button.clone {:type "button"} "Modify Experiment"]
   [:h2 "Treatment"]
   (%with treatment
	  [:p [:b "Name: "] (% name)]
	  [:p [:b "Description: "] (% description)]
	  [:p [:b "Tags: "] (% tags)])
   [:h2 "Instruments used"]
   [:div {:class "instrument-sublist"}
    (%each instruments
	   [:div {:class "instrument-sublist-view"}
	    [:div.inst-name (% name) "&nbsp;(" (% src) ")"]
	    [:div.inst-description (% description)]])]
   [:h2 "Schedule"]
   [:div.schedule "Schedule view TBD"]])


;; JOURNAL

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

;; COMMENT

(deftemplate comment-short-view
  [:div.comment-short
   [:p.comment-text (% content)]
   [:p.comment-sig
    (%strcat "@" (% username))
    " at " (% date-str)]])





