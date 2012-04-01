(ns experiment.views.bootstrap
  (:use
   noir.core
   hiccup.core
   handlebars.templates)
  (:require
   [hiccup.form-helpers :as hf]
   [clojure.string :as str]))

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

(deftemplate modal-form-dialog-template
  [:div
   [:div.modal-header 
    [:a.close {:data-dismiss "modal"} "x"]
    (%code header)]
   [:div.modal-body
    (%code body)]
   [:div.modal-footer
    (%code footer)]])

(defn modal-dialog [id & {:keys [header body footer] :as parts}]
  (modal-dialog-template (assoc parts :id id)))

(defn popover-link [text href pop-title pop-content]
  [:a {:class "popover-link"
       :href href
       :rel "popover"
       :data-original-title pop-title
       :data-content pop-content}
   text])
       


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

;; ## Pagination

(defn pagination [names active-offset]
  [:div.pagination
   [:ul
    (map-indexed (fn [name idx]
                   (if (= idx active-offset)
                     [:li {:class "active"}
                      [:a {:href "#"} name]]
                     [:li 
                      [:a {:href "#"} name]]))
                 names)]])

(defn pager [left-name right-name & [sides?]]
  [:ul.pager
   [:li {:class (if sides? "previous" "")}
    [:a {:href "#"} left-name]]
   [:li {:class (if sides? "next" "")}
    [:a {:href "#"} right-name]]])

;; ## Render Nav Menus
(defn- active-class [match current & {:keys [base] :or {base ""}}]
  (if (and match (= match current)) (str "active " base) base))

(defn- merge-props [orig over]
  (if-let [new-class (:class over)]
    (merge orig (assoc over :class (str/join \space [(:class orig) new-class])))
    (merge orig over)))

(declare dropdown-submenu)

(defn- menu-item [active {:keys [tag name href aprops lprops submenu] :as entry}]
  (if-not submenu
    [:li (merge-props {:class (active-class active tag)} lprops)
     (when name
       [:a (merge-props {:href href} aprops)
        name])]
    [:li (merge-props {:class (active-class active tag :base "dropdown")}
                      lprops)
     [:a (merge-props {:class "dropdown-toggle"
                       :href href
                       :data-toggle "dropdown"}
                      aprops)
      name
      [:b.caret]]
     (dropdown-submenu nil submenu)]))

(defelem dropdown-submenu [active submenu]
  [:ul.dropdown-menu
   (map (partial menu-item active)
        (filter #(not (nil? %)) submenu))])

(defelem nav-menu [menu active]
  [:ul.nav
   (map (partial menu-item active)
        (filter #(not (nil? %)) menu))])

               
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

   
