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


;; Menus
;; ------------------------

(defelem dropdown-item [name entries]
  (list
   [:li.dropdown
    [:a.dropdown-toggle {:data-toggle "dropdown"} name [:b.caret]]]
   [:ul.drop-down-menu
    entries]))
               
;; WIDGETS
;; ------------------------

(defn carousel [id items & [active-index]]
  [:div.carousel {:id id}
   [:div.carousel-inner
    (doall
     (map (fn [i item]
            [:div {:class (if (if active-index
                                (= i active-index)
                                (= i 1))
                            "item active" "item")}
             item])
          (range 1 (+ (count items) 1))
          items))]
   [:a.carousel-control.left {:href (str "#" id) :data-slide "prev"} "&lsaquo;"]
   [:a.carousel-control.right {:href (str "#" id) :data-slide "next"} "&rsaquo;"]])

   
