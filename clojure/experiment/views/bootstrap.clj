(ns experiment.views.bootstrap
  (:use
   noir.core
   hiccup.core
   handlebars.templates)
  (:require
   [hiccup.form-helpers :as hf]))

;; Twitter Bootstrap Widgets and Partials
;; ----------------------------------------


;; ## Generates both client and server templates

(deftemplate modal-dialog-template 
  [:div
;;   {:id (% id)
;;    :class "modal hide fade"
;;    :style "display:none;"}
   [:div.modal-header 
    [:a.close {:data-dismiss "modal"} "x"]
    (%code header)]
   [:div.modal-body
    (%code body)]
   [:div.modal-footer
    (%code footer)]])

(defn modal-dialog [id & {:keys [header body footer] :as parts}]
  (modal-dialog-template (assoc parts :id id)))

;; FORMS
;; ------------------------

(defn form-to [& args]
  (apply hf/form-to args))

(defelem ctrl-group [[name id] & controls]
  [:div.control-group
   [:label.control-label {:for id} name]
   [:div.controls
    controls]])

(defelem input [type id value]
  [:input {:type type :id id :value value}])

(defelem help-text [text]
  [:p.help-block text])

               
   