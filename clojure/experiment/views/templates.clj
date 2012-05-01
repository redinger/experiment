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

;; # Breadcrumbs

(deftemplate breadcrumbs-view
  [:ul.breadcrumb
   (%each path
          [:li {:class (% class)}
           [:a {:href (% url)} (% name)]
           [:span.divider "/"]])
   (%with tail
          [:li {:class "active"}
           [:a {:href (% url)} (% name)]])])

;; # Pagination

(deftemplate pagination-view
  [:ul
   (%each this
          [:li {:class (% class)} [:a {:href "#"} (% text)]])])

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
            
;; Related Objects List

(deftemplate related-objects
  [:div.related-objects
   [:table.table
    [:thead
     [:th
      [:td "Type"]
      [:td "Name"]]]
    [:tbody
     (%each this
            [:tr
             [:td (% type)]
             [:td (% name)]])]]])

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

(defn tag-list []
  [:div.tags
   (%each tags
    [:span.label.label-info (% this)] "&nbsp;")])
  

(deftemplate treatment-list-view
  [:div.result.treatment-list-view
   [:h3 [:a.title {:href "#" :data-id (% id)} ;; (%str "/explore/view/" (% type) "/" (% id)) }
         [:i.icon-screenshot] " " (% name)]]
   [:p (% description)]
   [:div.tags
    (%each tags
           [:span.label.label-info (% this)] "&nbsp;")]])

(deftemplate treatment-view
  [:div.treatment-view.object-view
   [:div.row
    [:div.page-header
     [:div.span7
      [:h1 (% name)]]
     [:div.span4
      [:span.pull-right
       [:button.btn.btn-large.btn-primary.experiment {:type "button"} "Experiment"]
       (%if owner [:button.btn.btn-large.edit {:type "button"} "Edit"])
       [:button.btn.btn-large.clone {:type "button"} "Clone"]]]
     [:div {:style "clear:both;"}]]]
   [:div.row
    [:div.span5
     [:p [:h3 "Protocol"]]
     [:p (%code description-html)]
     [:p [:b "Behavior"]]
     [:p
      [:span "Onset period of " (% dynamics.onset) " days, &nbsp;"]
      [:span "Washout period of " (% dynamics.washout) " days"]]
     [:p [:b "Tags "]
      [:a.add-tag {:href "#"} [:i.icon-plus-sign]]]
     [:p.tags
      (%each tags
             [:span.label.label-info (% this)] "&nbsp;")]
     [:div#discuss]]
    [:div.span1 [:p]]
    [:div.span6
     ;; Related, Discussion
     [:div#related]]
;;     [:div.row.conversations [:h2 "Conversations"]]
     ]])

(deftemplate treatment-editor
  [:div.treatment-edit.object-editor
   [:div.row
    [:div.page-header
     [:div.span7
      (%if name [:h1 "Editing " (% name)])
      (%unless name [:h1 "Create a new Treatment"])]
     [:div.span4]
     [:div {:style "clear:both"}]]]
   [:div.row
    [:div.span12
     [:div#editForm]]]
   [:div.row
    [:div.span3 [:p]]
    [:div.span5
     [:button.btn.btn-large.btn-success.accept.pull-left
      {:type "button"}
      (%if name "Update")
      (%unless name "Create")]
     [:button.btn.btn-large.btn-danger.cancel.pull-right
      {:type "button"} "Cancel"]]]])

;; INSTRUMENT

(deftemplate instrument-list-view
  [:div.result.instrument-list-view
   [:h3 [:a.title {:href "#" :data-id (% id)} ;; (%str "/explore/view/" (% type) "/" (% id)) }
         [:i.icon-eye-open {:style "vertical-align:middle"}] " " (% variable) " (" (% service) ")"]]
   [:p (% description)]
   [:p.tags
    (%each tags
      [:span.label.label-info (% this)] "&nbsp;")]])


(deftemplate instrument-short-table
  [:div {:class "instrument-short-table"}
   [:ul
    (%each instruments
	   [:li
	    [:a {:href (%strcat "/app/search/instrument/" (% id))}
	     [:span {:class "variable"} (% name)]
	     [:span {:class "type"} (% service)]]])
    ]])

(deftemplate instrument-view
  [:div.instrument-view
   [:div.row
    [:div.page-header
     [:div.span8
      [:h1 (% variable) " -- " [:a {:href "/account/services"} (% service)]]]
     [:div.span3
      [:span.pull-right
       (%if owner [:button.btn.btn-large.edit "Edit"])
       (%if tracked [:button.btn.btn-large.untrack "Untrack"])
       (%unless tracked [:button.btn.btn-large.track "Track"])]]
     [:div {:style "clear:both;"}]]]
   [:div.row
    [:div.span5
     [:p [:b "Description"]
      [:p (%code description-html)]]
     [:p [:b "Tags "]
      [:a.add-tag {:href "#"} [:i.icon-plus-sign]]]
     [:p.tags
      (%each tags
             [:span.label.label-info (% this)] "&nbsp;")]]
    [:div.span1 [:p]]
    [:div.span6
     ;; Related, Discussion
     ]]])
     

(deftemplate instrument-editor
  [:div.instrument-edit.object-editor
   [:div.row
    [:div.page-header
     [:div.span7
      [:h1 "Editing " (% variable) " -- " (% service)]]
     [:div.span4]
     [:div {:style "clear:both"}]]]
   [:div.row
    [:div.span12
     [:div#editForm]]]
   [:div.row
    [:div.span3 [:p]]
    [:div.span5
     [:button.btn.btn-large.btn-success.accept.pull-left
      {:type "button"} "Update"]
     [:button.btn.btn-large.btn-danger.cancel.pull-right
      {:type "button"} "Cancel"]]]])

;; EXPERIMENT
   
(deftemplate experiment-list-view
  [:div.result.experiment-list-view
   [:h3 [:a.title {:href "#" :data-id (% id)} ;; (%str "/explore/view/" (% type) "/" (% id)) }
        [:i.icon-random] " " (% treatment.name)]]
   [:p (% title)]
   [:ul
    (%each instruments
           [:li (% variable) "(measured by " (% service) ")"])]])

(deftemplate experiment-view
  [:div.experiment-view
   [:div.row
    [:div.page-header
     [:div.span8
      [:h1 {:href ""}
       (% treatment.name)]]
     [:div.span3
      [:span.pull-right
       [:button.btn.btn-primary.btn-large.run {:type "button"} "Run"]
       (%if owner [:button.btn.btn-large.edit {:type "button"} "Edit"])
       [:button.btn.btn-large.clone {:type "button"} "Clone"]]]
     [:div {:style "clear:both;"}]]]
   [:div.row
    (%with treatment
      [:div.span5
       [:p [:b "Treatment"]
        [:p (%code description-html)]]
       [:p [:b "Tags: "]
        [:a.add-tag {:href "#"} [:i.icon-plus-sign]]]
       [:p.tags
        (%each tags
               [:span.label.label-info (% this)] "&nbsp;")]])
    [:div.span1 [:p]]
    [:div.span6]]])
;;   [:h2 "Schedule"]
;;   [:div.schedule "Schedule view TBD"]])


;; COMMENT

(deftemplate comment-short-view
  [:div.comment-short
   [:p.comment-text (% content)]
   [:p.comment-sig
    (%strcat "@" (% username))
    " at " (% date-str)]])





