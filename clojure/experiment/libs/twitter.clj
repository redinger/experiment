(ns experiment.libs.twitter
  (:use noir.core
        hiccup.core
        experiment.infra.models)
  (:require [oauth.client :as oauth]
            [noir.response :as resp]
            [experiment.infra.session :as session]
            [experiment.infra.services :as services]
            [experiment.libs.properties :as props]
            [oauth.signature :as sig]
            [clj-http.client :as http]))
            
(services/register-oauth
 :tw
 ["Twitter"
  :description "Twitter connections will allow you to power instruments from your twitter stream by using microformats.  You will also be able to engage in social sharing of your experimental activities via Twitter (all this TBD)"]
 :title "Twitter"
 :url "http://personalexperiments.org/api/svc/twitter/authorize")

;;
;; Configuration
;;

(def consumer
  (oauth/make-consumer (props/get :twitter.key)
                       (props/get :twitter.secret)
                       "https://api.twitter.com/oauth/request_token"
                       "https://api.twitter.com/oauth/access_token"
                       "https://api.twitter.com/oauth/authorize"
                       :hmac-sha1))


;;
;; 3-Legged Bookkeeping & debug support
;;

;; Book keeping across phases

(defn save-req-tokens [request]
  (when (session/active?)
    (session/put! "withings_oauth_token" (:oauth_token request))
    (session/put! "withings_oauth_secret" (:oauth_token_secret request))))

(defn get-req-token []
  (when (session/active?)
    (session/get "withings_oauth_token")))

(defn get-req-secret []
  (when (session/active?)
    (session/get "withings_oauth_secret")))

(defn save-access-tokens [response]
  (if (session/active?)
    (update-model!
     (assoc (session/current-user)
       :tw-cred response))
    (update-model!
     (assoc (fetch-model :user :where {:username "eslick"})
       :tw-cred response))))
  
(defn get-access-token [user]
  (get-in user [:tw-cred :oauth_token]))

(defn get-access-secret [user]
  (get-in user [:tw-cred :oauth_token_secret]))

(defn get-user-id [user]
  (get-in user [:tw-cred :user_id]))

(defn get-user-screen-name [user]
  (get-in user [:tw-cred :screen_name]))

;;
;; GET TOKENS AND SEND USER TO AUTHORIZE PAGE
;;


(defn build-authorize-uri [target]
  (str (:authorize-uri consumer)
       "?"
       (http/generate-query-string 
        (assoc (sig/oauth-params consumer (get-req-token))
          :oauth_signature (sig/sign consumer
                                     (:authorize-uri consumer)
                                     (get-req-secret))
          :oauth_callback target))))

(defn oauth-link [name target]
  (save-req-tokens
   (oauth/request-token consumer target))
  [:a {:class "oauth"
       :href (build-authorize-uri target)}
   name])

(defpage [:get "/api/svc/twitter/oauth"] {:as request}
  (html
   (oauth-link "Oauth Twitter"
               "http://personalexperiments.org/api/svc/twitter/authorize")))

;;
;; AFTER AUTHORIZATION, USE REQUEST TOKENS TO GET ACCESS TOKENS
;;

(defn signing-uri [params]
  (sig/base-string "POST" (:access-uri consumer) params))
;;       "?oauth_verifier=" verifier))

(defn access-params [verifier]
  (let [params (assoc (sig/oauth-params consumer (get-req-token))
                 :oauth_verifier verifier)]
    (assoc params
      :oauth_signature (sig/sign consumer
                                 (signing-uri params)
                                 (get-req-secret)))))

(defn fetch-access-token [verifier]
  (let [params (access-params verifier)]
    (oauth/form-decode
     (:body
      (http/post (:access-uri consumer)
                 {:query-params params
                  :headers {"Authorization" (oauth/authorization-header (sort params))}})))))

(defpage [:get "/api/svc/twitter/authorize"] {:keys [oauth_token oauth_verifier] :as request}
  (clojure.tools.logging/spy ["OAUTH approval uri" request])
  (assert (= (get-req-token) oauth_token))
  (save-access-tokens (fetch-access-token oauth_verifier))
  (resp/redirect "/account/services"))
