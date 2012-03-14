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
(defn- include-js-libs
  "Development versions of JS libraries"
  []
  (if (= (props/get :mode) :dev)
    (include-js "/js/vendor/jquery-1.7.js"
                "/js/vendor/jquery.autoSuggest.js"
                "/js/vendor/bootstrap.js"
                "/js/vendor/d3.js"
                "/js/vendor/d3.time.js"
                "/js/qi-chart.js"
                "/js/vendor/handlebars.1.0.0.beta.3.js"
                "/js/vendor/underscore-131.js"
                "/js/vendor/backbone-091.js"
                "/js/vendor/backbone-forms.js"
                "/js/vendor/backbone-forms-bootstrap.js"
                "/js/dialog.js"
                )
    (include-js "/js/vendor/jquery-1.7.min.js"
                "/js/vendor/jquery.autoSuggest.packed.js"
                "/js/vendor/bootstrap.min.js"
                "/js/vendor/d3.min.js"
                "/js/vendor/d3.time.min.js"
                "/js/qi-chart.js"
                "/js/vendor/handlebars.1.0.0.beta.3.js"
                "/js/vendor/underscore-min-131.js"
                "/js/vendor/backbone-min-091.js"
                "/js/vendor/backbone-forms.js"
                "/js/vendor/backbone-forms-bootstrap.js"
                "/js/dialog.js"
                )))


;;
;; Standard Public Page Layout
;; --------------------------------------


;; ## Page Components
(defpartial render-footer []
  [:footer.footer
   [:div.container.footer-bar
    [:p
     [:a {:class "footer-link" :href "/article/terms"} "Terms of Use"] "|"
     [:a {:class "footer-link" :href "/article/privacy"} "Privacy"]"|"
     [:a {:class "footer-link" :href "/article/about"} "About"]]
    [:p [:small "[This site is best viewed on <a href='http://firefox.com'>Firefox 8+</a>, <a href='http://apple.com/safari/'>Safari 5+</a> or <a href='http://www.google.com/chrome'>Chrome 14+</a>]"]]]])

(defn default-nav []
  (if (session/logged-in?)
    {:menu [{:name "Home" :href "#"}
            {:name "Dashboard" :href "#dashboard"}
            {:name "About" :href "#about"}]
     :ctrl [{:name "Logout" :href "/action/logout"}]
     :search true}
    {:menu [{:name "Home" :href "#"}
            {:name "About" :href "#about"}
            {:name "Contact" :href "#contact"}]
     :ctrl [{:name "Register" :href "#registerModal"
             :props {:class "register-button"}}
            {:name "Login" :href "#loginModal"
             :props {:class "login-button btn-primary"}}]}))
  

(declare render-dialogs)

;; ## Default Layout

(defpartial page-frame [title & body-content]
  (html5
   [:head
    (layout-header title)
    (include-css-libs)
    (google/include-analytics)
    (facebook/include-jsapi)]
   [:body {:style "padding-top:60px; padding-bottom:40px;"}
    body-content
    (render-footer)
    (render-dialogs)
    (include-js-libs)]))
    
(defpartial layout [title nav & content]
  (page-frame
   title
   (nav-fixed nav)
   content))

;; ## Simple pages
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
   (render-template "modal-login-template"
                    (get-template "modal-login-template"))])
                

  
      