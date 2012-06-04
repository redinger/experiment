(ns experiment.model.omh-client
  (:use
   experiment.models.samples)
  (:require
   [clj-time.core :as time]
   [experiment.models.user :as user]
   [experiment.libs.datetime :as dt]
   [clj-json.core :as json]
   [clj-http.client :as http]))


;; CLIENT
;; ----------------------------

(def requester "PersonalExperiments.org")

(def default-endpoint
  {:root "http://localhost:8080/omh"
   :ver "v1.0"
   :token "32fe7db5-6b67-473b-ae99-801f8ec8fab2"})

(defn set-default-endpoint [default]
  (alter-var-root #'default-endpoint (fn [old] default)))

(defn- gen-url [endpoint cmd]
  (str (:root endpoint) "/" (:ver endpoint) cmd))

(defn- do-get [endpoint cmd & {:as params}]
  (http/get (gen-url endpoint cmd)
            {:query-params
             (assoc params
               :auth_token (:token endpoint))}))

(defn- do-post [endpoint cmd & {:as params}]
  (http/post (gen-url endpoint cmd)
             {:query-params
              (assoc params
                :auth_token (:token endpoint))}))

(defn- parse-response [response]
  (if (= (:status response) 200)
    (json/parse-string (:body response) true)
    (throw (java.lang.Error. (:status response)))))

;; Authenticate
(defn- do-authenticate-request [endpoint user password]
  (do-post endpoint "/authenticate"
          :username user
          :password password
          :requester requester))

(defn authenticate
  ([endpoint user password]
     (let [response (do-authenticate-request endpoint user password)]
       (parse-response (json/parse-string (:body response) true)))
  ([user password]
     (authenticate default-endpoint user password)))
  
;; Status
(defn status
  ([endpoint]
     (parse-response (do-get endpoint "/status")))
  ([]
     (status default-endpoint)))


;; Testing
;; ---------------------------------
  
(defn test-error []
  (do-get default-endpoint "/status2"))

(defn test-auth-error []
  (authenticate default-endpoint "eslick2" "foo"))