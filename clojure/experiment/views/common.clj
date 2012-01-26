(ns experiment.views.common
  (:use noir.core
        hiccup.core
        hiccup.form-helpers
        hiccup.page-helpers
        handlebars.templates)
  (:require [experiment.models.user :as user]
	    [experiment.infra.auth :as auth]
	    [experiment.infra.session :as session]
	    [experiment.infra.models :as models]
	    [clj-json.core :as json]
	    [noir.request :as req]
	    [noir.response :as resp]))

(defn- include-vendor-libs-dev []
  (include-js "/js/vendor/jquery-1.7.js"
	      "/js/vendor/jquery.autoSuggest.js"
	      "/js/vendor/jquery.simplemodal-1.4.1.js"
	      "/js/vendor/jquery.sparkline.min.js"
	      "/js/vendor/highcharts.src.js"
	      "/js/vendor/handlebars.1.0.0.beta.3.js"
	      "/js/vendor/underscore.js"
	      "/js/vendor/backbone.js"
	      "/js/dialog.js"
	      ))
	      
(defn- include-vendor-libs-prod []
  (include-js "/js/vendor/jquery-1.7.min.js"
	      "/js/vendor/jquery.autoSuggest.packed.js"
	      "/js/vendor/jquery.simplemodal.1.4.1.min.js"
	      "/js/vendor/jquery.sparkline.min.js"
	      "/js/vendor/highcharts.js"
	      "/js/vendor/handlebars.1.0.0.beta.3.js"
	      "/js/vendor/underscore-min.js"
	      "/js/vendor/backbone-min.js"
	      "/js/dialog.js"
	      ))

(defn- simple-js []
  (include-js "/js/vendor/jquery-1.7.min.js"
	      "/js/vendor/jquery.simplemodal.1.4.1.min.js"
	      "/js/vendor/handlebars.1.0.0.beta.3.js"
	      "/js/dialog.js"))

(defn render-analytics []
  "<script type=\"text/javascript\">

  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', 'UA-11911393-4']);
  _gaq.push(['_setDomainName', 'none']);
  _gaq.push(['_setAllowLinker', true]);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();

</script>")

(defn include-vendor-libs []
  (include-vendor-libs-dev))

(defpartial include-standard-css []
  (include-css "/css/reset.css")
  (include-css "/css/dialog.css")
  (include-css "/css/autoSuggest.css")
  (include-css "/css/calendar.css")
  (include-css "/css/main.css")
  (include-css "/css/app.css"))

(defpartial standard-head-nojs [& head-content]
  [:head
   [:title "Personal Experiments"]
   (include-standard-css)
   head-content
   (render-analytics)])

(defpartial standard-head [& head-content]
  [:head
   [:title "Personal Experiments"]
   (include-standard-css)
   (include-vendor-libs)
   head-content
   (render-analytics)])

;; Simple Layout

(defpartial layout [& content]
  (html5
   ;; Header
   (standard-head
    (include-js "/js/test1.js"))
   ;; Body
   [:body
    [:div#wrapper
     content]]))

(defpartial render-modal-dialog-skeleton []
  [:div {:id "dialog-modal-content"}
   [:div {:class "dialog-modal-title"}]
   [:div {:class "close"}
    [:a {:href "#" :class "simplemodal-close"} "x"]]
   [:div {:class "dialog-modal-data"}]])

;; ====================================
;; Standard Public Page Layout
;; ====================================

(deftemplate login-dialog-body 
  (form-to [:post "/action/login"]
	   [:div {:class "form-pair username-field"}
	    (label "username" "Username") (text-field "username")]
	   [:div {:class "form-pair password-field"}
	    (label "password" "Password") (password-field "password")]
	   [:div {:class "buttons"}
	    [:input {:type "submit" :name "submit" :value "Login"}]
	    [:input {:type "submit" :name "cancel" :value "Cancel"}]]))

(deftemplate register-dialog-body 
  (form-to [:post "/action/register"]
           [:div {:class "form-pair username-field"}
	    (label "email" "E-mail") (text-field "email")]
           [:div {:class "form-pair username-field"}
	    (label "name" "Your Name (optional)") (text-field "username")]
           [:div {:class "form-pair username-field"}
	    (label "username" "Username") (text-field "username")]
	   [:div {:class "form-pair password-field"}
	    (label "password" "Password") (password-field "password")]
	   [:div {:class "form-pair password-field"}
	    (label "password2" "Confirm Password") (password-field "password2")]
           (hidden-field "target" (% target))
           (hidden-field "default" (% default))
	   [:div {:class "buttons"}
	    [:input {:type "submit" :name "submit" :value "Login"}]
	    [:input {:class "cancel-button" :type "submit" :name "cancel" :value "Cancel"}]]))


(defpartial render-dialog-templates []
  [:div.templates {:style "display: none;"}
   (inline-template "login-dialog-body"
			      login-dialog-body
			      "text/x-jquery-html")
   (inline-template "register-dialog-body"
			      register-dialog-body
			      "text/x-jquery-html")])

;; (defpartial render-modal-dialog [title body-fn]
;;   (assert (string? title))
;;   [:div {:id "dialog-modal-content"}
;;    [:div {:class "dialog-modal-title"} title]
;;    [:div {:class "close"}
;;     [:a {:href "#" :class "simplemodal-close"} "x"]]
;;    [:div {:class "dialog-modal-data"}
;;     (body-fn)]])

;; (defn render-login-dialog []
;;   (render-modal-dialog
;;    "Login"
;;    (fn []
;;      (html
;;       (form-to [:post "/action/login"]
;; 	       [:div {:class "form-pair username-field"}
;; 		(label "username" "Username") (text-field "username")]
;; 	       [:div {:class "form-pair password-field"}
;; 		(label "password" "Password") (password-field "password")]
;; 	       [:div {:class "buttons"}
;; 		[:input {:type "submit" :name "submit" :value "Login"}]
;; 		[:input {:type "submit" :name "cancel" :value "Cancel"}]])))))

(defpartial render-footer []
  [:div.clear]
  [:div.footer
   [:div.footer-bar
    [:a {:class "footer-link" :href "/article/terms"} "Terms of Use"] "|"
    [:a {:class "footer-link" :href "/article/privacy"} "Privacy"]"|"
    [:a {:class "footer-link" :href "/article/about"} "About"]
    [:br]
    [:p "[This site is best viewed on <a href='http://firefox.com'>Firefox 8+</a>, <a href='http://apple.com/safari/'>Safari 5+</a> or <a href='http://www.google.com/chrome'>Chrome 14+</a>]"]]])

(defpartial render-header-menu []
  (if (session/logged-in?)
    [:div {:class "header-menu-bar"}
     [:a {:class "header-link" :href "/app/dashboard"}
      "Dashboard"]
     [:span {:class "separator"} "|"]
     [:a {:class "header-link logout-link" :href "/action/logout"}
      "Logout"]]

    [:div {:class "header-menu-bar"}
     [:a {:class "header-link register-link"
          :href "/action/register"}
      "Register"]
     [:span {:class "separator"} "|"]
     [:a {:class "header-link login-link"
          :href "/action/login"}
      "Login"]]))

(defpartial simple-layout [{:as options :keys [header-menu] :or {header-menu true}}
                           & content]
  (html5
   (standard-head-nojs)
   [:body
    ;; Main page content
    [:div#wrapper
     [:div.home-header-wrap
      [:div.home-header
       [:div.home-header-title
	[:a {:class "header-link" :href "/"} "PersonalExperiments"]]
       (when (:header-menu options)
         (render-header-menu))]]
     [:div.home-main
      content]
     (render-footer)]
    ;; Dialogs
    (render-modal-dialog-skeleton)
    (render-dialog-templates)
    ;; Home javascript
    (simple-js)]))

;; ====================================
;; Rich application layout
;; ====================================

(defpartial render-profile-summary []
  (let [user (session/current-user)]
    (if (session/logged-in?)
      (list [:img {:src "/img/generic_headshot.jpg"}]
		   ;; "https://gp1.wac.edgecastcdn.net/801245/socialcast.s3.amazonaws.com/tenants/6255/profile_photos/499368/Ian_Headshot_Zoom_square70.jpg"}]
	    [:h1 (or (:username user) "NO NAME!")]
	    (link-to "/app/profile" "Edit Profile")
	    [:br]
	    (link-to "/action/logout" "Logout")
	    [:br]
	    [:a {:class "help" :href "#"} "Help"])
      (list [:span
	     [:a {:class "login-link" :href "/action/login"}
	      "Login"]]
	    [:a {:class "help" :href "#"} "Help"]))))


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
	    (let [base (str "/app/" name)]
	      [:li {:class "menuitem"}
	       [:a {:href base :class (if subitems "expand" "action")}
		content]
	       (when subitems (render-submenu base subitems))])))
	menu)])


(defpartial nav-layout [menu-content]
  [:div#nav-pane.left-side-bar
   [:div.profile-summary
    (render-profile-summary)]
   [:hr]
   [:div#main-menu.main-menu
    (render-menu menu-content)]
   [:div.nav-footer
    (image "/img/c3ntheme_logo.png" "C3N Logo")
    [:br]
    [:div {:style "text-align: center"}
     (link-to "/article/terms" "Terms of Use")
     "&nbsp; | &nbsp;"
     (link-to "/article/privacy" "Privacy")
     "&nbsp;"]]])


(defpartial app-pane-layout [app-pane]
  [:div.app-pane-wrap
   [:div#app-pane.app-pane
    [:div.inner-pad
     app-pane]]])


(defpartial share-pane-layout [share-pane]
  [:div#share-pane.right-side-bar
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
      (app-pane-layout app-pane)
      (nav-layout menu)
      (share-pane-layout share-pane)]
     [:div#footer]]
    (render-modal-dialog-skeleton)
    (include-vendor-libs)
    (include-js "/js/app.js")
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
    (resp/redirect "/app/dashboard")
    (simple-layout {}
     (form-to [:post "/action/login"]
	      [:ul.simple-form
	       (label "username" "User Name: ") (text-field "username" (:username user)) [:br]
	       (label "password" "Password: ") (password-field "password" (:password user)) [:br]
	       (if-let [targ (:target user)]
		 (hidden-field "target" (:target user)))
	       (submit-button {:class "submit"} "submit")]))))

(defpage do-login [:post "/action/login"] {:as user}
  (cond (:cancel user)
	(resp/redirect "/")
	(auth/login user)
	(do (println "Successful login by " user)
	    (if-let [targ (:target user)]
	      (do (println "Redirecting to target: " targ)
		  (resp/redirect targ))
	      (resp/redirect "/app/dashboard")))
	true
	(do (println "Failed login by " user)
	    (resp/redirect "/"))))

(defn valid-registration-rec? [{:keys [email username password password2]}]
  (cond (or (= (count email) 0) (not (re-find #"@" email)))
        [false "We require a valid email address"]
        (not (> (count username) 3))
        [false "We require a valid username longer than 3 characters"]
        (= (count password) 4)
        [false "You must use a password longer than 4 characters"]
        (not (= password password2))
        [false "Passwords didn't match"]
        true
        [true nil]))

(defpage do-register [:post "/action/register"] {:as user}
  (if (:cancel user)
    (resp/redirect (or (:default user) "/"))
    (let [[valid message] (valid-registration-rec? user)]
      (if valid
        (do (user/create-user! (:username user) (:password user) (:email user) (:name user))
            (auth/login user)
            (resp/redirect (or (:target user) "/app/dashboard")))
        (simple-layout {}
         [:h2 "Registration failed"]
         [:p message]
         [:a {:href (or (:default user) "/")}
          "Return to home page"])))))

(defpage do-logout "/action/logout" {}
  (session/clear!)
  (resp/redirect "/"))

(defpage show-map "/util/show-request" {}
  (simple-layout {}
   [:div (interpose
	  '[:br]
	  (clojure.string/split 
	   (with-out-str
	     (clojure.pprint/write
	      (noir.request/ring-request)))
	   #"\n"))]))

(defpage not-supported "/not-supported" {}
  (simple-layout {}
   [:h2 "Your browser is not supported by this site"]))
  
      