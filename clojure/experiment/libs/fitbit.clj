(ns experiment.libs.fitbit
  (:use noir.core
        hiccup.core
        experiment.infra.models)
  (:require [oauth.client :as oauth]
            [clojure.math.numeric-tower :as math]
            [experiment.libs.datetime :as dt]
            [clj-time.core :as time]
            [noir.response :as resp]
            [experiment.infra.session :as session]
            [oauth.signature :as sig]
            [clj-json.core :as json]
            [clj-http.client :as http]))


(def consumer-key "f9e277098d3a4bcfb2d63d5276d35e61")
(def consumer-secret "ee7c99506878461491154ebbdce2a3fb")

(def consumer
  (oauth/make-consumer consumer-key
                       consumer-secret
                       "http://api.fitbit.com/oauth/request_token"
                       "http://api.fitbit.com/oauth/access_token"
                       "http://api.fitbit.com/oauth/authorize"
                       :hmac-sha1))

(defn- save-req-tokens [request]
  (when (session/active?)
    (session/put! "fitbit_oauth_token" (:oauth_token request))
    (session/put! "fitbit_oauth_secret" (:oauth_token_secret request))))

(defn- get-req-token []
  (session/get "fitbit_oauth_token"))

(defn- get-req-secret []
  (:oauth_token_secret @request-auth))

(defn- save-access-tokens [response]
  (if (session/active?)
    (update-model!
     (assoc (session/current-user)
       :fit-cred response))
    (update-model!
     (assoc (fetch-model :user :where {:username "eslick"})
       :fit-cred response))))

(defn get-user-id [user]
  (get-in user [:fit-cred :encoded_user_id]))

(defn id->user [id]
  (fetch-model :user :where {"fit-cred.encoded_user_id" id}))

(defn get-user-uid [user]
  (if-let [uid (get-in user [:fit-cred :unique-id])]
    uid
    (let [uid (rand-int 1000000)]
      (modify-model! user {:$set {"fit-cred.unique-id" uid}})
      uid)))

(defn uid->user [uid]
  (fetch-model :user :where {"fit-cred.unique-id" uid}))

(defn- get-access-token [user]
  (get-in user [:fit-cred :oauth_token]))

(defn- get-access-secret [user]
  (get-in user [:fit-cred :oauth_token_secret]))


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

(defpage [:get "/api/fitbit/oauth"] {:as request}
  (html
   (oauth-link "Oauth Fitbit"
               "http://personalexperiments.org/api/fitbit/authorize")))


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

(defpage [:get "/api/fitbit/authorize"] {:keys [oauth_token oauth_verifier] :as request}
  (assert (= (get-req-token) oauth_token))
  (save-access-tokens (fetch-access-token oauth_verifier))
  (resp/redirect "/app/profile"))

;;
;; Make Fitbit Requests
;;

(defn request
  ([op user cmd]
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
  (modify-model! user {:$set {"fit-cred.subscribed" true}}))

(defn unsubscribe [user]
  (request :delete user (str "apiSubscriptions/" (get-user-uid user) ".json"))
  (modify-model! user {:$unset {"fit-cred.subscribed" true}}))

(defn list-subscriptions [user]
  (request :get user "apiSubscriptions.json"))

(defpage fitbit-update [:post "/api/fitbit/update"] {:as params}
  (clojure.tools.logging/spy params)
  (when-let [fn @notification-handler]
    (map fn (:updates params))))
                 


