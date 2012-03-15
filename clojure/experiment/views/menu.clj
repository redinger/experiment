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

(defn- user-dropdown [user]
  (when-let [{:keys [name options]} user]
    [:ul.nav
     [:li.dropdown
      [:a.dropdown-toggle {:href "#" :data-toggle "dropdown"} name [:b.caret]]
      [:ul.dropdown-menu
       (filter #(not (nil? %)) options)]]]))

(defn- controls [controls active]
  (when controls
    [:ul.nav
     (for [{:keys [name href props]} controls]
       [:li {:class (if (= active name) "active" "")}
        [:a.btn.btn-navbar
         (merge {:href href} props)
         name]])]))

(defn active-class [match current]
  (if (= match current) "active" ""))

(defpartial nav-fixed [menu]
  [:div.navbar.navbar-fixed-top
   [:div.navbar-inner
    [:div.container 
     [:a.brand {:href "/"}
      ;;      [:img {:src "/img/favicon.ico"
      ;;             :style "height:20px;width:20px;margin-right:10px;"}]
      "Personal Experiments"]
     [:ul.nav
      (for [{:keys [name href props]}
            (filter #(not (nil? %)) (:menu menu))]
        [:li {:class (active-class (:active menu) name)}
         [:a (assoc props :href href) name]])]
     [:div.pull-right
      (bar-search (:search menu))
      (user-dropdown (:user menu))
      (controls (:ctrl menu) (:active menu))]]]])

(defpartial subnav-fixed [nav]
  (when-let [nav (:subnav nav)]
    [:div.subnav.subnav-fixed-top
     [:div.nav-inner
      [:div.container
       [:ul.nav.nav-pills
        (map (fn [{:keys [name href]}]
               [:li {:class (active-class (:active nav) name)}
                [:a {:href href} name]])
             (:menu nav))]]]]))


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
