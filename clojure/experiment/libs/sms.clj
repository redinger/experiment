(ns experiment.libs.sms
  (:use noir.core
	experiment.infra.models)
  (:require [clj-http.client :as http]
	    [clojure.contrib.string :as string]
	    [clj-time.core :as time]
	    [clojure.data.json :as json]
	    [somnium.congomongo :as mongo]
	    [noir.response :as response]
	    [noir.request :as request]
	    [clojure.tools.logging :as log]))

;;
;; An SMS gateway based on the grouptexting API
;;


;; CREDENTIALS

(defonce ^:dynamic *credentials* nil)

(defn set-credentials [credentials]
  (assert (and (:user credentials) (:pw credentials)))
  (alter-var-root #'*credentials* (fn [a b] b) credentials))

(defn- inject-perms [map]
  (let [{user :user pw :pw} *credentials*]
    (assert (and user pw))
    (merge map {:user user :pass pw})))

(defmacro with-credentials [[creds] & body]
  `(binding [*credentials* (or ~creds *credentials*)]
     ~@body))

;; BUILD REQUESTS

(defn- compose-request-url
  "Compose a map of args as key-value pairs on the base URL"
  [base argmap]
  (log/spy
   (apply str base
	  "?"
	  (http/generate-query-string
	   (inject-perms argmap)))))

;;
;; API: SEND MESSAGES
;;


(defn send-sms [number subject message & [credentials]]
  (assert (< (count message) 160))
  (assert (not (re-find #"[']" message)))
  (with-credentials [credentials]
    (http/get (compose-request-url
	       "https://app.grouptexting.com/api/sending"
	       {:phonenumber number
		:subject subject
		:message message}))))

;;
;; API: INBOX HANDLER
;;

(defonce ^:dynamic *handler* nil)

(defn set-reply-handler [handler]
  (alter-var-root #'*handler* (fn [a b] b) handler))

(defn- handle-reply [from message]
  (when (or (fn? *handler*) (var? *handler*))
    (*handler* from message)))
  
(defpage inbox-url [:get "/sms/receive"] [{:as request}]
  (let [params (:params request)]
    (handle-reply (:from params) (:message params))
    (response/empty)))

;;
;; API: Account mgmt
;;

(defn account-balance [& [credentials]]
  (with-credentials [credentials]
    (http/get (compose-request-url
	       "https://app.grouptexting.com/api/credits/check"
	       {}))))
	       
	     
