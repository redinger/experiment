(ns experiment.views.templates
  (:use
   experiment.infra.models
   noir.core
   hiccup.core
   hiccup.page-helpers
   hiccup.form-helpers
   handlebars.templates)
  (:require
   [noir.response :as response]
   [experiment.views.bootstrap :as boot]))


;; # Dynamic Template Loader API endpoint

(defpage load-template [:get "/api/templates/:id"]
  {:keys [id]}
  (response/content-type
   "text/html"
   (html-template
    (get-template id))))


;;
;; Template Views for System Objects
;;

;; # Instrument Templates

(deftemplate journal-page
  [:div.row.journal-page
   [:div.span7.jvl]
   [:div.span5.jvp]])

(deftemplate journal-view
  [:div.well
   [:div.journal-header
    [:span.pull-left
     [:p {:style "text-align: center;"} (% date-str)]]
    [:span.pull-right
     [:span#purpose.btn-group
      [:a.btn.btn-mini.dropdown-toggle
       {:data-toggle "dropdown"
        :href "#" 
        :style "margin-right: 10px;"}
       (% annotation) " " [:span.caret]]
      [:ul.dropdown-menu
       [:li [:a.option {:href "#"} "Note"]]
       [:li [:a.option {:href "#"} "Change"]]
       [:li [:a.option {:href "#"} "Adverse"]]]]
     "&nbsp;"
     [:span#sharing.btn-group
      [:a.btn.btn-mini.dropdown-toggle
       {:data-toggle "dropdown"
        :href "#" 
        :style "margin-right: 10px;"}
       (% sharing) " " [:span.caret]]
      [:ul.dropdown-menu
       [:li [:a.option {:href "#"} "Private"]]
       [:li [:a.option {:href "#"} "Friends"]]
       [:li [:a.option {:href "#"} "Public"]]]]]
    [:div.clear]]
   [:form {:style "clear: both;"}
    [:hr]
    (boot/ctrl-group
     ["Tagline" "short"]
     (boot/input {:id "journal-short" :maxlength "40"} "text" "short" (% short)))
    (boot/ctrl-group
     ["Long Entry" "content"]
     (boot/textarea {:rows "15"
                     :cols "80"
                     :class "input-xlarge"
                     :id "journal-content"
                     :style "resize: none;"}
                    "content"
                    (% content)))]])

(deftemplate journal-list
  [:div
   [:span [:h2.pull-left "Journal Entries"]
    [:span.pull-right [:button.btn.btn-primary.new "New Entry"]]]
   [:table.table
    [:thead
     [:tr
      [:th "Date"]
      [:th "Description"]
      [:th "Type"]
      [:th "Sharing"]]]
    [:tbody
     (%each journals
            [:tr {:data (% id)}
             [:td.time
              (% date-str)]
             [:td.short
              (% short)]
             [:td.anno
              (% annotation)]
             [:td.sharing
              (% sharing)]
             [:td.del
              [:button.btn.btn-mini.btn-danger.del
               [:i.icon-remove.icon-white]]]
             ])]]])
            

;; TIMELINE

(deftemplate timeline-header
  [:div#timeline-header {:style "height: 60px;"}
   [:div.daterange.pull-left
    [:input {:type "text" :name "rangepicker" :value (% range) :id "rangepicker"}]]
   [:div.pull-right
    [:div.btn-group
     [:a.btn.dropdown-toggle {:data-toggle "dropdown" :href "#"}
      "Visibility "
      [:span.caret]]
     [:ul.dropdown-menu]]]])
    

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


;; COMMENT

(deftemplate comment-short-view
  [:div.comment-short
   [:p.comment-text (% content)]
   [:p.comment-sig
    (%strcat "@" (% username))
    " at " (% date-str)]])





