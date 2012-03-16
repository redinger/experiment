(ns experiment.libs.withings
  (:use noir.core
        hiccup.core
        experiment.infra.models)
  (:require [oauth.client :as oauth]
            [clojure.math.numeric-tower :as math]
            [experiment.libs.datetime :as dt]
            [clj-time.core :as time]
            [noir.response :as resp]
            [experiment.infra.session :as session]
            [experiment.libs.properties :as props]
            [oauth.signature :as sig]
            [clj-json.core :as json]
            [clj-http.client :as http]))

(def consumer
  (oauth/make-consumer (props/get :withings.key)
                       (props/get :withings.secret)
                       "https://oauth.withings.com/account/request_token"
                       "https://oauth.withings.com/account/access_token"
                       "https://oauth.withings.com/account/authorize"
                       :hmac-sha1))

;; Book keeping across phases

(defn save-req-tokens [request]
  (when (session/active?)
    (session/put! "withings_oauth_token" (:oauth_token request))
    (session/put! "withings_oauth_secret" (:oauth_token_secret request))))

(defn get-req-token []
  (session/get "withings_oauth_token"))

(defn get-req-secret []
  (session/get "withings_oauth_secret"))


(defn save-access-tokens [userid response]
  (modify-model!
   (session/current-user)
   {:$set
    {:services
     {:wi {:userid userid
           :token (:oauth_token response)
           :secret (:oauth_secret response)}}}}))
  
(defn get-userid [user]
  (get-in user [:services :wi :userid]))

(defn get-access-token [user]
  (get-in user [:services :wi :token]))

(defn get-access-secret [user]
  (get-in user [:services :wi :secret]))


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

(defpage [:get "/api/withings/oauth"] {:as request}
  (html
   (oauth-link "Oauth Withings"
               "http://personalexperiments.org/api/withings/authorize")))


;;
;; AFTER AUTHORIZATION, USE REQUEST TOKENS TO GET ACCESS TOKENS
;;

(defn signing-uri [params]
  (sig/base-string "POST" (:access-uri consumer) params))

(defn access-params [verifier]
  (let [params (sig/oauth-params consumer (get-req-token))
        params (assoc params
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

(defpage [:get "/api/withings/authorize"] {:keys [oauth_token oauth_verifier] :as request}
  (assert (= (get-req-token) oauth_token))
  (save-access-tokens (fetch-access-token oauth_verifier))
  (resp/redirect "/app/profile"))

;; ===================================================================

;;
;; STANDARD QUERY OF PROTECTED RESOURCES
;;

(defn action
  ([user service action params]
     (assert (map? params))
     (let [uri (format "http://wbsapi.withings.net/%s" service)
           params (merge {:action action
                          :userid (get-userid user)}
                         params)
           access-creds (oauth/credentials
                         consumer
                         (get-access-token user)
                         (get-access-secret user)
                         :GET
                         uri
                         params)]
       (json/parse-string
        (:body
         (http/get uri
                   {:query-params (merge access-creds params)}))
        true)))
  ([user service action]
     (action user service action {})))



;; =======================
;; API
;; =======================

(def type-table
  {1 :weight
   4 :height
   5 :lbm
   6 :fat-ratio
   8 :fat-mass
   9 :dbp
   10 :sbp
   11 :hr})

(defn translate-type [rec]
  (get type-table (:type rec)))

(def convert-table
  {:weight (fn [value] (* value 2.2))})

(defn convert-value [type measure]
  (let [fn (convert-table type)
        raw (* (:value measure) (math/expt 10 (:unit measure)))]
    (if fn (fn raw) (float raw))))

(defn filter-record [rec]
  (when (= (:category rec) 1)
    (map (fn [measure]
           (let [type (translate-type measure)]
             {:date (:date rec)
              :type type
              :value (convert-value type measure)}))
         (:measures rec))))
    
(defn filter-records
  "Return a vector of the last updated time and an array of measures"
  [response]
  (let [body (:body response)]
    [(:updatetime body)
     (mapcat filter-record (:measuregrps body))]))
   
(defn user-measures
  ([user startdate]
     (filter-records
      (action user "measure" "getmeas"
              {:startdate (dt/as-utc startdate) :devtype 1})))
  ([user]
     (filter-records
      (action user "measure" "getmeas"
              {:devtype 1}))))

(defn user-info [user]
  (action user "user" "getbyuserid" {}))

