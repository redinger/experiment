(ns experiment.views.authviews
  (:use
   noir.core
   hiccup.core
   hiccup.page-helpers
   experiment.views.common
   experiment.views.bootstrap
   [hiccup.form-helpers :exclude [form-to input]]
   handlebars.templates
   experiment.infra.api)
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


;; ## LOGIN Support
;; The default login page re-uses the dialog logic

(defn login-with-redirect [target]
  (resp/redirect (str "/action/login?target=" target)))

(defpage login [:get "/action/login"]
  {:keys [target] :as args}
  (layout
   "Login to Access Protected Area"
   (default-nav)
   [:div {:style "height:400px"}
    [:script "$(document).ready(function () { window.loginModal.show(); });"]]))
  
(defapi do-login [:post "/action/login"]
  {:as user}
  (resp/json
   (if-let [user (auth/login user)]
     {:result "success"}
     {:result "fail"
      :message "Username/E-mail not found or password was incorrect"})))

(defpage do-logout "/action/logout" {:as options}
  (session/clear!)
  (resp/redirect (or (:target options) "/")))


;; ## Protect access to areas of the site

;; NOTE: be sure to adjust this to open up dashboard soon
(defn handle-private-route []
  (let [uri (:uri (noir.request/ring-request))]
    (when-not (session/logged-in?)
      (login-with-redirect uri))))
  

(pre-route "/" {}
           (when (session/logged-in?)
             (if (user/is-admin?)
               (resp/redirect "/dashboard")
               (resp/redirect "/study1"))))

(pre-route "/dashboard*" {}
           (handle-private-route))

;; Other protected routes
;; - /settings

(pre-route "/admin*" {}
	   (when-not (session/logged-in?)
	     (let [uri (:uri (noir.request/ring-request))]
	       (println "Redirecting to login, not authorized for: " uri)
	       (login-with-redirect uri))))


;; ## REGISTRATION Support

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

(defapi do-register [:post "/action/register"] {:as user}
  (let [[result message] (valid-registration-rec? user)]
    (if result
      {:result "success"}
      {:result "fail"
       :message message})))

(defapi check-username [:get "/action/check-username"]
  {:keys [username] :as data}
  (println username)
  {:exists (if (models/fetch-model :user {:username username})
             "true"
             "false")})

(defapi check-email [:get "/action/check-email"]
  {:keys [email] :as data}
  (println email)
  {:exists (if (models/fetch-model :user {:email email})
             "true" "false")})


;; ## FORGOTTEN Password
(defapi do-forgot-password [:post "/action/forgotpw"] {:as options}
  (if-let [user (user/get-user (:username options))]
    (try
      (mail/send-message-to (:email user)
                            {:subject "Forgot Password"
                             :body "Sorry, we can only reset your password manually for now.  Please reply to this e-mail with a suggested password.  Shortly we'll allow profile editing and can fix your forgotten password more easily."})
      {:result "success"}
      (catch java.lang.Throwable e
        {:result "fail"
         :message (format "Internal error: %s please write to eslick@media.mit.edu" e)}))
    {:result "fail"
     :message "Did not recognize your username or e-mail address"}))


;; (defapi reset-password [:get "/action/forgotpw"]

