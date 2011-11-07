(ns experiment.libs.sms
  (:use noir.core
	experiment.infra.models)
  (:require [clj-http.client :as http]
	    [clojure.contrib.string :as string]
	    [clj-time.core :as time]
	    [clojure.data.json :as json]
	    [somnium.congomongo :as mongo]
	    [noir.response :as response]
	    [noir.request :as request]))

;; An SMS gateway based on the grouptexting API

;; {:user "username" :pw "password"}
(defonce ^:dynamic *credentials* nil)

;;
;; Simple send interface
;;

(def ^:dynamic *send-url* "https://app.grouptexting.com/api/sending")

(defn- url-encode [val] val)

(defn- get-param [[key value]]
  (format "%s=%s" (name key) (url-encode value)))

(defn- compose-get-request [base argmap]
  (apply str base
	 (interleave
	  (cons "?" (repeat (- (count argmap) 1) "&"))
	  (map get-param argmap))))

(defn- send-url [user pass number subject message]
  (assert (< (count message) 160))
  (assert (not (re-find #"[']" message))) 
  (compose-get-request *send-url*
		       {:user user
			:pass pass
			:phonenumber number
			:subject subject
			:message message}))

(defn set-credentials [credentials]
  (assert (and (:user credentials) (:pw credentials)))
  (alter-var-root #'*credentials* (fn [a b] b) credentials))

(defonce *send-agent* (agent {}))

(defn- send [agent number subject message credentials]
  (let [{user :user pw :pw} (or credentials *credentials*)]
    (println user)
    (assert (and user pw))
    (let [result (http/get (send-url user pw number subject message))]
      (println (:body result))
      (java.lang.Thread/sleep 500)
      result)))

(defn send-sms [number subject message & [credentials]]
  (send-off *send-agent* send number subject message credentials)
  nil)

;;
;; Simple inbox handler
;;

(defonce *handler* nil)

(defn set-reply-handler [handler]
  (alter-var-root #'*handler* (fn [a b] b) handler))

(defn- handle-reply [from message]
  (when (or (fn? *handler*) (var? *handler*))
    (*handler* from message)))
  
(defpage inbox-url [:get "/sms/receive"] [{:as request}]
  (let [params (:params request)]
    (handle-reply (:from params) (:message params))
    (-> (response "OK")
	(status 200))))