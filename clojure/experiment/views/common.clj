(ns experiment.views.common
  (:use noir.core
        noir.content.css
        hiccup.core
        hiccup.form-helpers
        hiccup.page-helpers)
  (:require [experiment.models.user :as user]
	    [experiment.infra.auth :as auth]
	    [experiment.infra.session :as session]
	    [experiment.infra.models :as models]
	    [clj-json.core :as json]
	    [noir.response :as resp]))

(defn- include-vendor-libs-dev []
  (include-js "/js/vendor/jquery-1.7.js"
	      "/js/vendor/jquery.autoSuggest.js"
	      "/js/vendor/handlebars.1.0.0.beta.3.js"
	      "/js/vendor/underscore.js"
	      "/js/vendor/backbone.js"
	      "/js/vendor/jquery.sparkline.min.js"
	      "/js/vendor/highcharts.src.js"))
	      
(defn- include-vendor-libs-prod []
  (include-js "/js/vendor/jquery-1.7.min.js"
	      "/js/vendor/jquery.autoSuggest.packed.js"
	      "/js/vendor/handlebars.1.0.0.beta.3.js"
	      "/js/vendor/underscore-min.js"
	      "/js/vendor/backbone-min.js"
	      "/js/vendor/jquery.sparkline.min.js"
	      "/js/vendor/highcharts.js"))

(defn include-vendor-libs []
  (include-vendor-libs-dev))

(defpartial include-standard-css []
  (include-css "/css/reset.css")
;;  (include-css "/css/jquery.backbone.widgets.css")
  (include-css "/css/autoSuggest.css")
;;  [:style {:type "text/css"} (noir-css)]
  (include-css "/css/layout.css"))

(defpartial standard-head-nojs [& head-content]
  [:head
   [:title "Experiments.com"]
   (include-standard-css)
   head-content])

(defpartial standard-head [& head-content]
  [:head
   [:title "My Experiments"]
   (include-standard-css)
   (include-vendor-libs-dev)
   head-content])

;; Content layouts

(defpartial layout [& content]
  (html5
   ;; Header
   (standard-head
    (include-js "/js/test1.js"))
   ;; Body
   [:body
    [:div#wrapper
     content]]))

(defpartial simple-layout [& content]
  (html5
   (standard-head)
   [:body
    [:div#wrapper
     content]]))

;; Application layouts

(defpartial render-profile-summary []
  (let [user (session/current-user)]
    (if (session/logged-in?)
      (list [:img {:src "https://gp1.wac.edgecastcdn.net/801245/socialcast.s3.amazonaws.com/tenants/6255/profile_photos/499368/Ian_Headshot_Zoom_square70.jpg"}]
	    [:h1 (or (:name user) "NO NAME!")]
	    (link-to "/app/profile" "Edit Profile")
	    [:br]
	    (link-to "/action/logout" "Logout"))
      (list [:span
	     (link-to "/action/login" "Login")]))))

(defpartial render-submenu [parent menu]
  [:ul {:class "sublist"}
   (map (fn [[name content]]
	  [:li {:class "subitem"}
	   [:a {:href (str parent "/" name)} content]])
	menu)])

(defpartial render-menu [menu]
  [:ul {:class "menulist"}
   (map (fn [[name content & subitems]]
	  (when name
	    (let [base (str "/app/" name)]
	      [:li {:class "menuitem"}
	       [:a {:href base} 
		content]
	       (when subitems (render-submenu base subitems))])))
	menu)])

(defpartial nav-layout [menu-content]
  [:div#nav-pane
   [:div#profile-summary
    (render-profile-summary)]
   [:hr]
   [:div#main-menu
    (render-menu menu-content)]
   [:div#nav-footer
    (image "/img/c3ntheme_logo.png" "C3N Logo")
    [:br]
    [:div {:style "text-align: center"}
     (link-to "/article/terms" "Terms of Use")
     "&nbsp; | &nbsp;"
     (link-to "/article/privacy" "Privacy")
     "&nbsp;"]]])

(defpartial app-pane-layout [app-pane]
  [:div#app-pane
   app-pane])

(defpartial share-pane-layout [share-pane]
  [:div#share-pane
   share-pane])

(defn bootstrap-collection-expr [name coll]
  (str name ".reset("
       (json/generate-string
	(models/export-model coll))
       ");"))

(defn bootstrap-instance-expr [name coll]
  (str name ".set("
       (json/generate-string
	(models/export-model coll))
       ");"))

(defpartial send-user []
  [:script {:type "text/javascript"}
   (bootstrap-instance-expr "window.User" (session/current-user))])

(defpartial app-layout [menu app-pane share-pane bootstrap-data]
  (html5
   (standard-head-nojs)
   [:body
    [:div#wrapper
     [:div#main
      (nav-layout menu)
      (app-pane-layout app-pane)
      (share-pane-layout share-pane)]
     [:div#footer]]
    (include-vendor-libs)
    (include-js "/js/models.js")
    (include-js "/js/app.js")
    (include-js "/js/create.js")
    (send-user)
    bootstrap-data]))

      

;; Authentication
;;
;; Require session authentication for main site
;; and for the admin subsection

(pre-route "/app*" {}
	   (let [uri (:uri (noir.request/ring-request))]
	     (println "Handling: " uri)
	     (when-not (session/logged-in?)
	       (println "Redirecting to login, not authorized for: " uri)
	       (resp/redirect (str "/action/login?target=" uri)))))

(pre-route "/admin*" {}
	   (when-not (session/logged-in?)
	     (let [uri (:uri (noir.request/ring-request))]
	       (println "Redirecting to login, not authorized for: " uri)
	       (resp/redirect (str "/action/login?target=" uri)))))

(defpage login "/action/login" {:as user}
  (if (session/logged-in?)
    (resp/redirect "/app")
    (simple-layout
     (form-to [:post "/action/login"]
	      [:ul.simple-form
	       (label "username" "User Name: ") (text-field "username" (:username user)) [:br]
	       (label "password" "Password: ") (password-field "password" (:password user)) [:br]
	       (if-let [targ (:target user)]
		 (hidden-field "target" (:target user)))
	       (submit-button {:class "submit"} "submit")]))))

(defpage do-login [:post "/action/login"] {:as user}
  (if (auth/login user)
    (do (println "Successful login by " user)
	(if-let [targ (:target user)]
	  (do (println "Redirecting to target: " targ)
	      (resp/redirect targ))
	  (resp/redirect "/app/dashboard")))
    (do (println "Failed login by " user)
	(render "/action/login" user))))

(defpage do-logout "/action/logout" {}
  (session/clear!)
  (resp/redirect "/"))

(defpage show-map "/util/show-request" {}
  (simple-layout
   [:div (interpose
	  '[:br]
	  (clojure.string/split 
	   (with-out-str
	     (clojure.pprint/write
	      (noir.request/ring-request)))
	   #"\n"))]))

(defpage not-supported "/not-supported" {}
  (simple-layout
   [:h2 "Your browser is not supported by this site"]))
  
      