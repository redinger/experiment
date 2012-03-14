(ns experiment.views.menu
  (:use
   noir.core
   hiccup.core
   experiment.views.bootstrap
   hiccup.page-helpers
   [hiccup.form-helpers :exclude [form-to input]])
  (:require
   [experiment.infra.session :as session]))

;; --------------------------------------------------------
;; Generic Menus (I hope) for site frame and other pages
;; --------------------------------------------------------

(defpartial nav-fixed [menu]
  [:div.navbar.navbar-fixed-top
   [:div.navbar-inner
    [:div.container 
     [:a.brand {:href "/"}
;;      [:img {:src "/img/favicon.ico"
;;             :style "height:20px;width:20px;margin-right:10px;"}]
      "Personal Experiments"]
     [:ul.nav.nav-tabs
      [:li.active 
       (for [{:keys [name href class]} (:menu menu)]
         [:li 
          [:a {:data-toggle "tab"
               :href href
               :class (or class "")}
           name]])]]
     [:div.pull-right
      (when (:search menu)
        [:form.navbar-search {:action ""}
         [:input.span2 {:type "text"}]])
      [:ul.nav
       (for [{:keys [name href props]} (:ctrl menu)]
         [:li
          [:a.btn
           (assoc props :href href)
           name]])]]]]])


;; ## Application Window Menu
(defpartial render-submenu [parent menu]
  [:ul {:class "submenu" :style "display: none;"}
   (map (fn [[name content]]
	  [:li {:class "subitem"}
	   [:a {:href (str parent "/" name) :class "action"}
	    content]])
	menu)])

(defpartial render-menu [menu]
  [:ul ;; {:class "menulist"}
   (map (fn [[name content & subitems]]
	  (when name
	    (let [base (str name)]
	      [:li {:class "menuitem"}
	       [:a {:href base :class (if subitems "expand" "action")}
		content]
	       (when subitems (render-submenu base subitems))])))
	menu)])
