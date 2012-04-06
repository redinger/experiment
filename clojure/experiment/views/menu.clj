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

(defn- bar-search [search]
  (when search
    [:form.navbar-search {:action ""}
     [:input.span2 {:type "text"}]]))

;; ## Layout for Site Nav Bar
(defpartial nav-fixed [menu]
  [:div.navbar.navbar-fixed-top
   [:div.navbar-inner
    [:div.container 
     [:a.brand {:href "/"}
      "Personal Experiments"]
     (nav-menu (:main menu) (:active menu))
     [:div.pull-right
      (bar-search (:search menu))
      (nav-menu (:ctrl menu) (:active menu))]]]])

(defpartial subnav-fixed [menu]
  (when menu
    [:div.subnav.subnav-fixed-top
     [:div.nav-inner
      [:div.container
       (nav-menu {:class "nav nav-pills"} (:menu menu) (:active menu))]]]))

(defn- breadcrumb [{:keys [name href props]}]
  [:li props
   [:a {:href href} name]
   [:span.divider]])
  
(defpartial breadcrumbs [crumbs]
  [:ul.breadcrumb (map breadcrumb crumbs)])


;; ## Application Window Menu
(defpartial render-submenu [parent menu]
  [:ul {:class "submenu" :style "display: none;"}
   (keep (fn [[name content]]
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
