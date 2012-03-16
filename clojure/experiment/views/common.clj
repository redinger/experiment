(ns experiment.views.common
  (:use
   noir.core
   hiccup.core
   hiccup.page-helpers
   handlebars.templates
   experiment.views.bootstrap
   experiment.views.menu)
  (:require 
   [clojure.tools.logging :as log]
   [clj-json.core :as json]
   [noir.request :as req]
   [noir.response :as resp]
   [experiment.libs.properties :as props]
   [experiment.libs.mail :as mail]
   [experiment.infra.auth :as auth]
   [experiment.infra.session :as session]
   [experiment.infra.models :as models]
   [experiment.models.user :as user]
   [experiment.views.google :as google]
   [experiment.views.facebook :as facebook]
   ))

;; View Framework
;; -----------------------
;;
;; This file contains the major page building blocks for our Bootstrap-based
;; page architecture.  This provides for common page elements such as headers
;; footers, includes, top level menu navigation as well as registration and login
;; and markup for modal dialogs, etc.
;;

;; ## Header Fields
(defn layout-header [title]
  (list
   [:title title]
   [:meta {:name "author" :content "Ian Eslick"}]
   [:meta {:name "description" :content "Personal Experiments"}]
   [:link {:rel "shortcut icon" :href "/img/favicon.ico"}]
   [:meta {:http-equiv "X-UA-Compatible" :content "chrome=1"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]))


;; ## CSS Libraries
(defpartial include-css-libs []
  (include-css "/css/bootstrap.css")
  (include-css "/css/override.css")
  (include-css "/css/autoSuggest.css")
  (include-css "/css/calendar.css"))

;; ## Javascript Libraries
(defn- include-jquery
  []
  (if (= (props/get :mode) :dev)
    [:script {:type "text/javascript", :src "http://code.jquery.com/jquery-1.7.1.js"}]
    [:script {:type "text/javascript", :src "http://code.jquery.com/jquery-1.7.1.min.js"}]))

(defn- include-js-libs
  "Development versions of JS libraries"
  []
  (if (= (props/get :mode) :dev)
    (include-js "/js/vendor/jquery.autoSuggest.js"
                "/js/vendor/bootstrap.js"
                "/js/vendor/d3.js"
                "/js/vendor/d3.time.js"
                "/js/qi-chart.js"
                "/js/vendor/handlebars.1.0.0.beta.3.js"
                "/js/vendor/underscore-131.js"
                "/js/vendor/backbone-091.js"
                "/js/vendor/backbone-forms.js"
                "/js/vendor/backbone-forms-bootstrap.js"
                "/js/vendor/jquery-ui-editors.js"
                "/js/dialog.js"
                )
    (include-js "/js/vendor/jquery.autoSuggest.packed.js"
                "/js/vendor/bootstrap.min.js"
                "/js/vendor/d3.min.js"
                "/js/vendor/d3.time.min.js"
                "/js/qi-chart.js"
                "/js/vendor/handlebars.1.0.0.beta.3.js"
                "/js/vendor/underscore-min-131.js"
                "/js/vendor/backbone-min-091.js"
                "/js/vendor/backbone-forms.js"
                "/js/vendor/backbone-forms-bootstrap.js"
                "/js/vendor/jquery-ui-editors.js"
                "/js/dialog.js"
                )))


;;
;; Support Standard Page Layouts and Structure
;; -----------------------------------------

(defn nav-user-name [& [user]]
  (let [user (or user (session/current-user))]
    (or (:name user) (:username user))))

(defn- research-submenu []
  [{:name "Authoring Study" :href "/study1"}
;;   {:name "Site Analysis" :href "/article/analysis"}
   {:lprops {:class "divider"}}
   {:name "New Media Medicine" :href "http://newmed.media.mit.edu/"}])

(defn- user-submenu []
  [{:name '([:i.icon-cog] " Settings")
    :href "/settings"}
   {:name '([:i.icon-question-sign] " Help")
    :href "/help"}
   {:lprops {:class "divider"}}
   {:name '([:i.icon-off] " Logout")
    :href "/action/logout"}])

(defn default-nav [& [active]]
  (if-let [user (session/current-user)]
    {:nav
     {:active active
      :main [{:name "Dashboard" :href "/"}
             {:name "Explore"   :href "/explore"
              :aprops {:class "explore-link"}}
             {:name "Research" :href "#"
              :submenu (research-submenu)}
             {:name "About" :href "/article/about"}]
      :ctrl [{:name (nav-user-name user)
              :submenu (user-submenu)}]}}
;;     :crumbs [{:name "Home" :href "/"}
;;             {:name "Dashboard" :href "/dashboard"}]}
    {:nav
     {:active active
      :main [{:name "Home" :href "/"}
             {:name "Research" :href "#"
              :submenu (research-submenu)}
             {:name "About" :href "/article/about"}]
      :ctrl [{:name "Register" :href "#registerModal"
              :aprops {:class "register-button"}}
             {:name "Login" :href "#loginModal"
              :aprops {:class "login-button"}}]}}))


;; ## Page Components

(defpartial render-footer []
  [:footer.footer
   [:div.container.footer-bar
    [:p
     [:a {:class "footer-link" :href "/article/terms"} "Terms of Use"] "|"
     [:a {:class "footer-link" :href "/article/privacy"} "Privacy"]"|"
     [:a {:class "footer-link" :href "/article/about"} "About"]]
    [:p [:small "[This site is best viewed on <a href='http://firefox.com'>Firefox 8+</a>, <a href='http://apple.com/safari/'>Safari 5+</a> or <a href='http://www.google.com/chrome'>Chrome 14+</a>]"]]]])

  

;; ## Default Page

(declare render-dialogs)

(defpartial page-frame [[title & [fixed-size]] & body-content]
  (html5
   [:head
    (layout-header title)
    (include-jquery)
    (include-css-libs)
    (google/include-analytics)
    ]
   [:body {:style (str "padding-top:" (or fixed-size 40) "px; padding-bottom:40px;")}
;;    (facebook/include-jsapi)
    body-content
    (render-footer)
    (render-dialogs)
    (include-js-libs)]))
    
(defpartial layout [title nav & content]
  (page-frame [title (if (:subnav nav) 80 40)]
   (nav-fixed (:nav nav))
   (subnav-fixed (:subnav nav))
   (when-let [crumbs (:crumbs nav)]
     (breadcrumbs crumbs))
   content))


;; ## Simple content pages

(defpage not-supported "/not-supported" {}
  (layout
   "Browser Not Supported"
   (default-nav)
   [:div.content
    [:h2 "Your browser is not supported by this site"]]))

(defpage show-map "/util/show-request" {}
  (layout
   "Show Debug Request Map"
   (default-nav)
   [:div.content
    (interpose
     '[:br]
     (clojure.string/split 
      (with-out-str
        (clojure.pprint/write
         (noir.request/ring-request)))
      #"\n"))]))



;; Building blocks for rich-client applications
;; -----------------------------------------------

;; ## Serialize Models
(defn bootstrap-collection-expr [name coll]
  (str name ".reset("
       (json/generate-string
	(models/server->client coll))
       ");"))

(defn bootstrap-instance-expr [name coll]
  (str name ".set("
       (json/generate-string
	(models/server->client coll))
       ");"))

(defpartial send-user []
  [:script {:type "text/javascript"}
   (bootstrap-instance-expr "window.ExApp.User" (session/current-user))])


;; ## Send client-side templates
(defn render-template [id template]
  (inline-template
   id template "text/x-jquery-html"))

(defn render-all-templates
  ([]
     (map (fn [[name template]]
	    (render-template name template))
	  (all-templates)))
  ([names]
     (map (fn [[name template]]
	    (render-template name template))
	  (select-keys (all-templates) list))))

;; ## Common dialogs
      
(defn render-dialogs []
  [:div.templates
   (render-template "modal-dialog-template"
                    (get-template "modal-dialog-template"))
   (render-template "modal-form-dialog-template"
                    (get-template "modal-form-dialog-template"))])
                

  
      