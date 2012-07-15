(ns experiment.libs.fitbit
  (:use noir.core
        hiccup.core
        experiment.infra.models)
  (:require [clojure.math.numeric-tower :as math]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [clj-time.core :as time]
            [oauth.client :as oauth]
            [oauth.signature :as sig]
            [experiment.libs.datetime :as dt]
            [noir.response :as resp]
            [experiment.libs.properties :as props]
            [experiment.infra.session :as session]
            [experiment.infra.services :as services]
            [experiment.models.user :as user]
            ))

;; Service configuration
(services/register-oauth
 :fitbit 
 ["Fitbit"
  :description "Connect to your FitBit tracking data"]
 :title "Fitbit"
 :url "http://personalexperiments.org/api/svc/fitbit/authorize")


(def fitbit :fit)

(def consumer
  (oauth/make-consumer (props/get :fitbit.key)
                       (props/get :fitbit.secret)
                       "http://api.fitbit.com/oauth/request_token"
                       "http://api.fitbit.com/oauth/access_token"
                       "http://api.fitbit.com/oauth/authorize"
                       :hmac-sha1))

(assert (:key consumer))

(defn- save-req-tokens [request]
  (when (session/active?)
    (session/put! "fitbit_oauth_token" (:oauth_token request))
    (session/put! "fitbit_oauth_secret" (:oauth_token_secret request))))

(defn- get-req-token []
  (session/get "fitbit_oauth_token"))

(defn- get-req-secret []
  (session/get "fitbit_oauth_secret"))

(defn- save-access-tokens [response]
  (user/set-service (session/current-user) fitbit
                    {:token (:oauth_token response)
                     :secret (:oauth_token_secret response)}))

(defn get-user-id [user]
  (user/get-service-param user fitbit :encoded_user_id))

(defn id->user [id]
  (fetch-model :user :where {"services.fit.encoded_user_id" id}))

(defn get-user-uid [user]
  (if-let [uid (user/get-service-param user fitbit :unique-id)]
    uid
    (let [uid (rand-int 1000000)]
      (user/set-service-param user fitbit :unique-id uid)
      uid)))

(defn uid->user [uid]
  (fetch-model :user :where {"services.fit.unique-id" uid}))

(defn- get-access-token [user]
  (user/get-service-param user fitbit :token))

(defn- get-access-secret [user]
  (user/get-service-param user fitbit :secret))


;;
;; GET TOKENS AND SEND USER TO AUTHORIZE PAGE
;;


(defn- build-authorize-uri [target]
  (str (:authorize-uri consumer)
       "?"
       (http/generate-query-string 
        (assoc (sig/oauth-params consumer (get-req-token))
          :oauth_signature (sig/sign consumer
                                     (:authorize-uri consumer)
                                     (get-req-secret))
          :oauth_callback target))))

(defn- oauth-link [name target]
  (save-req-tokens
   (oauth/request-token consumer target))
  [:a {:class "oauth"
       :href (build-authorize-uri target)}
   name])

;; Manual Oauth Link
(defpage [:get "/api/svc/fitbit/oauth"] {:as request}
  (html
   (oauth-link "Oauth Fitbit"
               "http://personalexperiments.org/api/svc/fitbit/authorize")))

;;
;; AFTER AUTHORIZATION, USE REQUEST TOKENS TO GET ACCESS TOKENS
;;

(defn- signing-uri [params]
  (sig/base-string "POST" (:access-uri consumer) params))

(defn- access-params [verifier]
  (let [params (sig/oauth-params consumer (get-req-token))
        params (assoc params
                 :oauth_verifier verifier)]
    (assoc params
      :oauth_signature (sig/sign consumer
                                 (signing-uri params)
                                 (get-req-secret)))))

(defn- fetch-access-token [verifier]
  (let [params (access-params verifier)]
    (oauth/form-decode
     (:body
      (http/post (:access-uri consumer)
                 {:query-params params
                  :headers {"Authorization" (oauth/authorization-header (sort params))}})))))

(declare subscribe)

(defpage [:get "/api/svc/fitbit/authorize"] {:keys [oauth_token oauth_verifier] :as request}
  (assert (= (get-req-token) oauth_token))
  (save-access-tokens (fetch-access-token oauth_verifier))
  (subscribe (experiment.models.user/get-user {:username (session/current-user)}))
  (resp/redirect "/account/services"))

;;
;; Make Fitbit Requests
;;

(defn request
  ([op user cmd]
     (assert (:key consumer))
     (let [uri (str "http://api.fitbit.com/1/user/-/" cmd)
           creds (oauth/credentials
                  consumer
                  (get-access-token user)
                  (get-access-secret user)
                  op
                  uri)]
       (json/parse-string
        (:body (http/request
                {:method op
                 :url uri
                 :headers {"Authorization" (oauth/authorization-header creds)}}))
        true)))
  ([user cmd]
     (request :get user cmd)))
     
(defn summary [user date]
  (request user (format "activities/date/%s.json" (dt/as-iso-8601-date date))))


;;
;; Subscriptions
;;

(defonce notification-handler (atom nil))

(defn set-notification-handler [f]
  (swap! notification-handler (fn [old] f)))

(defn subscribed? [user]
  (get-in user [:fit-cred :subscribed]))

(defn subscribe [user]
  (request :post user (str "apiSubscriptions/" (get-user-uid user) ".json"))
  (modify-model! user {:$set {"services.fitbit.subscribed" true} :$inc {"updates" 1}}))

(defn unsubscribe [user]
  (request :delete user (str "apiSubscriptions/" (get-user-uid user) ".json"))
  (modify-model! user {:$unset {"services.fitbit.subscribed" true} :$inc {"updates" 1}}))

(defn list-subscriptions [user]
  (request :get user "apiSubscriptions.json"))

(defpage fitbit-updates [:post "/api/svc/fitbit/updates"] {:as params}
  (clojure.tools.logging/spy noir.request/*request*)
  (clojure.tools.logging/spy params)
  (try
    (when-let [fn @notification-handler]
      (map fn (:updates params)))
    (catch java.lang.Throwable e
      (clojure.tools.logging/spy "Error in fitbit-update")
      (clojure.tools.logging/spy e)))
  {:status 204 :body ""})
