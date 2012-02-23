(ns experiment.views.app
  (:require
   [clojure.data.json :as json]
   [noir.response :as resp]
   [experiment.infra.models :as models]
   [experiment.infra.session :as session]
   [experiment.models.user :as user]
   [experiment.models.profile]
   [experiment.models.events :as events])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers
	experiment.models.suggestions
        experiment.views.common
	handlebars.templates))

;; This file packages and renders most resources relevant to the
;; client application including client-side templates, data
;; bootstrapping and HTML skeletons.

;; See views/common.clj for the static page template and menu
;; rendering

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

(defn current-trials []
  (models/fetch-models
   :trial
   :where {:user (:username (session/current-user))}))

(defn menu-content []
  [["dashboard" "Dashboard"]
   (concat ["trials" "Trials"]
           (map (fn [model num]
                  [(:_id model) (str "Trial " num)])
                (current-trials)
                (range 1 10)))
   ["search" "Search"]
   (when (user/is-admin?)
     ["admin" "Admin"])])

(defpartial nav-layout []
  [:div#nav-pane.left-side-bar
   [:div.profile-summary
    (render-profile-summary)]
   [:hr]
   [:div#main-menu.main-menu
    (render-menu (menu-content))]
   [:div.nav-footer
    (image "/img/c3ntheme_logo.png" "C3N Logo")
    [:br]
    [:div {:style "text-align: center"}
     (link-to "/article/terms" "Terms of Use")
     "&nbsp; | &nbsp;"
     (link-to "/article/privacy" "Privacy")
     "&nbsp;"]]])

(defpartial app-pane []
  [:div.app-pane-wrap
   [:div#app-pane.app-pane
    [:div#app-wrap.inner-pad]]])

(defpartial bootstrap-data []
  (let [user (session/current-user)
	username (:username user)]
    [:script {:type "text/javascript"}
     (map #(apply bootstrap-collection-expr %)
	  [["window.ExApp.Instruments" (models/fetch-models :instrument)]
	   ["window.exApp.Experiments" (models/fetch-models :experiment)]
	   ["window.ExApp.MyTrials"
	    (models/fetch-models :trial :where {:user username})]
	   ["window.ExApp.Treatments" (models/fetch-models :treatment)]
	   ["window.ExApp.MyTrackers"
	    (models/fetch-models :tracker :where {:user.$id (:_id user)}
				 :only [:user :instrument :type :state])]
           ["window.ExApp.Users"
            (models/fetch-models :user :only [:username :type])]])
     (str "window.ExApp.Suggestions.reset("
	  (json/json-str (compute-suggestions))
	  ");")]))

(defpartial app-layout []
  (html5
   (standard-head-nojs)
   [:body
    [:div#wrapper
     [:div#app-main
      (app-pane)
      (nav-layout)
      [:div#share-pane.right-side-bar]
      [:div#footer]]]
    [:div#container
     (render-all-templates)
     (render-modal-dialog-skeleton)
     (include-vendor-libs)
     (include-js "/js/app.js")
     (send-user)
     (bootstrap-data)]]))

(defpage "/app*" {}
  (app-layout))


;;
;; Pre-alpha transition page
;;

(pre-route "/app*" {}
  (when (not (user/is-admin?))
    (resp/redirect "/coming-soon")))

(defpage "/coming-soon" {}
  (simple-layout {}
   [:div.main
    [:h2 "Thank you for registering"]
    [:p "PersonalExperiments.org will be available for use shortly.  We will send e-mails to all registered users when the site launches later in February.  In the meantime, your account enables you to participate in our first " [:a {:href "/study1"} "research study"] "."]]))