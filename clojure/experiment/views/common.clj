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
   [clojure.string :as str]
   [cheshire.core :as json]
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
   [:meta {:charset "utf-8"}]
   [:meta {:http-equiv "X-UA-Compatible" :content "chrome=1"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
   [:meta {:name "author" :content "Ian Eslick"}]
   [:meta {:name "description" :content "Personal Experiments"}]
   [:link {:rel "shortcut icon" :href "/img/favicon.ico"}]))


;; ## CSS Libraries
(defpartial include-css-libs []
  (include-css "/css/bootstrap.css")
  (include-css "/css/override.css")
  (include-css "/css/autoSuggest.css")
  (include-css "/css/calendar.css")
  (include-css "/css/smoothness/jquery-ui-1.8.18.custom.css"))

;; ## Javascript Libraries
(defpartial require-js [deps]
  [:script
   {:type "text/javascript"
    :src "/js/libs/require/require.js"
    :data-main "/js/load"}]
  (when-let [depstring (and deps (map #(format "'%s'" %) deps))]
    [:script
     {:type "text/javascript"}
     "require(["
     (str/join ", " (map #(format "'%s'" %) deps))
     "], function () { console.log('loaded dependencies for: " (format "%s" deps) "') });"]))

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
   {:name "MIT's New Media Medicine" :href "http://newmed.media.mit.edu/"}
   {:name "Lybba.org" :href "http://lybba.org"} 
   {:name "C3N Project At CCHMC" :href "Http://c3nproject.org"}])

(defn- user-submenu []
  [{:name '([:i.icon-cog] "Account")
    :href "/account"}
   {:name '([:i.icon-question-sign] " Help")
    :href "/help"}
   {:lprops {:class "divider"}}
   {:name '([:i.icon-off] " Logout")
    :href "/action/logout"}])

(defn default-nav [& [active]]
  (if-let [user (session/current-user)]
    {:nav
     {:active active
      :main [{:tag "dashboard" :name "Dashboard" :href "/"}
             {:tag "explore" :name "Explore"   :href "/explore"
              :aprops {:class "explore-link"}}
             {:name "Research" :href "#"
              :submenu (research-submenu)}
             {:tag "about" :name "About" :href "/article/about"}]
      :ctrl [{:name (nav-user-name user)
              :submenu (user-submenu)}]}}
;;     :crumbs [{:name "Home" :href "/"}
;;             {:name "Dashboard" :href "/dashboard"}]}
    {:nav
     {:active active
      :main [{:tag "home" :name "Home" :href "/"}
             {:tag "research" :name "Research" :href "#"
              :submenu (research-submenu)}
             {:tag "about" :name "About" :href "/article/about"}]
      :ctrl [{:tag "register" :name "Register" :href "#registerModal"
              :aprops {:class "register-button"}}
             {:tag "login" :name "Login" :href "#loginModal"
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

(defn render-spinner []
  [:div#spinner.spinner
   {:style "display:none"}
   [:img#img-spinner
    {:src "/img/spinner.gif" :alt "Loading"}]])

(declare render-dialogs)

(defpartial page-frame [[title & {:keys [fixed-size deps]}] & body-content]
  (html5
   [:head
    (layout-header title)
    (require-js deps)
    (google/include-analytics)
    (include-css-libs)
    ]
   [:body {:style (str "padding-top:" (or fixed-size 40) "px; padding-bottom:40px;")}
    body-content
    (render-footer)
    (render-dialogs)
    (render-spinner)]))

;;    (facebook/include-jsapi)
;;    (include-js-libs)
    
(defpartial layout [[title nav & rest] & content]
  (page-frame (vec
               (concat (list title :fixed-size (if (:subnav nav) 80 40))
                       rest))
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
(defn bootstrap-user-json []
  [:script {:type "text/json" :id "bootstrap-user"}
   (json/generate-string
    (models/server->client
     (session/current-user)))])

(defn bootstrap-models-json [models]
  [:script {:type "text/json" :id "bootstrap-models"}
   (json/generate-string
    (models/server->client models))])

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
                

  
      