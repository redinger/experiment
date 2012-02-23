(ns experiment.libs.withings
  (:use noir.core
        hiccup.core)
  (:require [oauth.client :as oauth]
            [clj-http.client :as http]))

(def oauth-key "417254733763bee16dbd130ff833fc66400098d51d84347377fcf47cf7d")
(def oauth-secret "608cf6e9c0759012cd11f6e3c2fe2784ccff157c4c3be625d5e56599d59")

(def consumer
  (oauth/make-consumer oauth-key
                       oauth-secret
                       "https://oauth.withings.com/account/request_token"
                       "https://oauth.withings.com/account/access_token"
                       "https://oauth.withings.com/account/authorize"
                       :hmac-sha1))


(defn oauth-link [name target]
  [:a {:class "oauth"
       :href (oauth/user-approval-uri
              consumer
              (oauth/request-token consumer "/api/withings/oauth"))}
   name])

(defpage [:get "/api/withings/oauth"] {:keys [target json-payload]}
  nil)

(defn wi-user [user]
  (:wi-userid user))

(defn wi-credentials [user]
  (let [cred (:wi-cred user)]
    (assert (:oauth_token cred))
    cred))
  
(defn wi-action
  ([user service action params]
     (assert (map? params))
     (let [uri (format "http://wbsapi.withings.net/%s" service)
           params (assoc params :userid (wi-user user))
           user-creds (wi-credentials user)
           access-creds (oauth/credentials
                         consumer
                         (:oauth_token user-creds)
                         (:oauth_token_secret user-creds)
                         :POST
                         uri
                         params)]
       (http/post uri :query-params (merge params access-creds))))
;;                  :parameters (http/post  {:use-expect-continue false}))))
  ([user service action]
     (wi-action user service action {})))

;; =======================
;; API
;; =======================

(defn translate-type [rec]
  ({1 :weight
    4 :height
    5 :lbm
    6 :fat-ratio
    8 :fat-mass
    9 :dbp
    10 :sbp
    11 :hr}
   (:type rec)))

(defn filter-record [rec]
  (when (= (:category rec) 1)
    (map (fn [measure]
           {:date (:date rec)
            :type (translate-type rec)
            :value (* (:value measure) (* 10 (:unit measure)))})
         (:measuregrps rec))))
           
    
(defn filter-records
  "Return a vector of the last updated time and an array of measures"
  [response]
  [(:updatetime (:body response))
   (mapcat filter-record (:measuregrps (:body response)))])
   
(defn user-measures
  ([user startdate]
     (assert (number? startdate))
     (filter-records
      (wi-action user "measure" "getmeas"
                 {:startdate startdate :devtype 1})))
  ([user]
     (user-measures user 0)))

(defn user-info [user]
  (wi-action user "user" "getbyuserid"))

                  