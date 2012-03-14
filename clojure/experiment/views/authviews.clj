(ns experiment.views.authviews
  (:use
   noir.core
   hiccup.core
   hiccup.page-helpers
   experiment.views.common
   experiment.views.bootstrap
   [hiccup.form-helpers :exclude [form-to input]]
   handlebars.templates)
  (:require
   [experiment.infra.session :as session]
   [experiment.infra.auth :as auth]
   [experiment.infra.models :as models]
   [experiment.models.user :as user]
   [experiment.libs.mail :as mail]
   [noir.response :as resp]))
   

;; Authentication
;; ----------------------------
;;
;; Backend support for login, registration, forgotten passwords, etc.

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

(defpage do-login [:post "/action/login"]
  {:as user}
  (resp/json
   (if-let [user (auth/login user)]
     {:result "success"}
     {:result "fail"
      :message "Username/E-mail not found or password was incorrect"})))

(defpage check-username [:get "/action/check-username"]
  {:keys [username] :as data}
  (println "check username" data)
  (resp/json
   {:exists (if (models/fetch-model :user {:username username})
              "true"
              "false")}))

(defpage check-email [:get "/action/check-email"]
  {:keys [email] :as data}
  (println "check email" data)
  (resp/json
   {:exists (if (models/fetch-model :user {:email email})
              "true" "false")}))

(defn valid-registration-rec? [{:keys [email username password password2]}]
  (cond (or (= (count email) 0) (not (re-find #"@" email)))
        [false "We require a valid email address"]
        (models/fetch-model :user {:email email})
        [false "Email address is already registered"]
        (models/fetch-model :user {:username username})
        [false (format "Username '%s' is already registered" username)]
        (not (> (count username) 3))
        [false "We require a valid username longer than 3 characters"]
        (= (count password) 4)
        [false "You must use a password longer than 4 characters"]
        (not (= password password2))
        [false "Passwords didn't match"]
        true
        [true nil]))

(def reg-notice-body "User '%s' (%s) just registered on personalexperiments.org")

(defn register-new-user [user]
  (user/create-user! (:username user) (:password user) (:email user) (:name user))
  (mail/send-site-message
   {:subject "New registration"
    :body (format reg-notice-body (:username user) (:name user))})
  (mail/send-message-to (:email user)
   {:subject "Thank you for registering at PersonalExperiments.org"
    :body (format "Your username is: %s
We will contact you shortly when the site or the site's study is ready to launch" (:username user))}))

(defpage do-register [:post "/action/register"] {:as user}
  (resp/json
   (let [[result message] (valid-registration-rec? user)]
     (if result
       {:result "success"}
       {:result "fail"
        :message message}))))

(defpage do-logout "/action/logout" {:as options}
  (session/clear!)
  (resp/redirect (or (:target options) "/")))

(defpage do-forgot-password [:post "/action/forgotpw"] {:as options}
  (resp/json
   (if-let [user (user/get-user (:username options))]
     (try
       (mail/send-message-to (:email user)
                             {:subject "Forgot Password Reminder"
                              :body "Sorry, we can only reset your password manually for now.  Please reply to this e-mail with a suggested password.  Shortly we'll allow profile editing and can fix your forgotten password more easily."})
       (throw (java.lang.Error. "Foobar!"))
       {:result "success"}
       (catch java.lang.Throwable e
         {:result "fail"
          :message (format "Internal error: %s please write to eslick@media.mit.edu" e)}))
     {:result "fail"
      :message "Did not recognize your username or e-mail address"})))


(defpartial login-form [user]
  (if (session/logged-in?)
    (resp/redirect "/")
    (layout
     "Login to Personal Experiments"
     (default-nav)
     [:div.content
      (form-to {:class "horizontal-layout"} [:post "/action/login"]
               [:ul.simple-form
                (label "username" "User Name: ") (text-field "username" (:username user)) [:br]
                (label "password" "Password: ") (password-field "password" (:password user)) [:br]
                (if-let [targ (:target user)]
                  (hidden-field "target" (:target user)))
                (submit-button {:class "submit"} "submit")])])))

;; ## Dialog Content Templates
(deftemplate modal-login-template
  [:div {:id "modalLogin" :class "modal hide fade"
         :style "display:none;"}
   [:div.modal-header
    [:a.close {:data-dismiss "modal"} "x"]
    [:h1 "Login"]]
   [:div.modal-body
    [:fieldset
     (ctrl-group {:class (%str "control-group " (% username-status))}
                 ["Username or E-mail" "username"]
                 (input "text" "username" (% username))
                 (help-text (% username-message)))
     (ctrl-group {:class (%str "control-group " (% password-status))}
                 ["Password" "password"]
                 (password-field (% password))
                 (help-text (% password-message)))]]
   [:div.modal-footer
    [:a.btn.btn-primary {:class "login"} "Login"]
    [:a.btn.btn-primary {:class "cancel"} "Cancel"]]])

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
           [:div {:class "form-pair email-field"}
	    (label "email" "E-mail") (text-field "email")]
           [:div {:class "form-pair username-field"}
	    (label "name" "Your Name (optional)") (text-field "name")]
           [:div {:class "form-pair name-field"}
	    (label "username" "Username") (text-field "username")]
	   [:div {:class "form-pair password-field"}
	    (label "password" "Password") (password-field "password")]
	   [:div {:class "form-pair password-field"}
	    (label "password2" "Confirm Password") (password-field "password2")]
           (hidden-field "target" (% target))
           (hidden-field "default" (% default))
	   [:div {:class "buttons"}
	    [:input {:type "submit" :name "submit" :value "Register"}]
	    [:input {:class "cancel-button" :type "submit" :name "cancel" :value "Cancel"}]]))

(deftemplate forgot-password-body
  (form-to [:post "/action/forgotpw"]
	   [:div {:class "form-pair username-field"}
	    (label "username" "Username or Email") (text-field "username")]
	   [:div {:class "buttons"}
	    [:input {:type "submit" :name "submit" :value "Lookup"}]
	    [:input {:type "submit" :name "cancel" :value "Cancel"}]]))

(defpartial render-dialog-templates []
  [:div.templates {:style "display: none;"}
   (inline-template "login-dialog-body"
			      login-dialog-body
			      "text/x-jquery-html")
   (inline-template "register-dialog-body"
			      register-dialog-body
			      "text/x-jquery-html")])

