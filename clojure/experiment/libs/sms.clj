(ns experiment.libs.sms
  (:use noir.core
	experiment.infra.models)
  (:require [clj-http.client :as http]
	    [clojure.contrib.string :as string]
	    [clj-time.core :as time]
            [experiment.libs.datetime :as dt]
	    [clojure.data.json :as json]
            [clojure.tools.logging :as log]
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
;; Message log
;;

(defn log-message! [message]
  (assert (and (:from message) (:message message)))
  (mongo/insert! :sms (assoc message
                        :date (dt/as-date (dt/now))
                        :type :sms)))

(defn get-messages
  ([from]
     (mongo/fetch :sms :where {:from from}))
  ([start end]
     (mongo/fetch :sms :where {:date {:$gte start :$lte end}}
                  :sort {:date 1})))

(defn get-latest-message [from]
  (first
   (mongo/fetch :sms :where {:from from}
                :sort {:date -1}
                :limit 1)))


;;
;; API: SEND MESSAGES
;;


(defn send-sms [number message & [credentials]]
  (assert (< (count message) 160))
  (assert (not (re-find #"[']" message)))
  (with-credentials [credentials]
    (http/get (compose-request-url
	       "https://app.grouptexting.com/api/sending"
	       {:phonenumber number
		:message message}))))

;;
;; API: INBOX HANDLER
;;

(defn default-handler [from message]
  (log/spy [from message]))

(defonce ^:dynamic *handler* #'default-handler)

(defn set-reply-handler [handler]
  (alter-var-root #'*handler* (fn [a b] b) handler))

(defn- handle-reply [from message]
  (let [ts (dt/now)]
    (log-message! {:from message :message message :ts (dt/as-utc ts)})
    (when (or (fn? *handler*) (var? *handler*))
      (*handler* ts from message))))
  
(defpage inbox-url [:get "/sms/receive"] {:keys [from message]}
  (handle-reply from message)
  (response/empty))

;;
;; API: Account mgmt
;;

(defn account-balance [& [credentials]]
  (with-credentials [credentials]
    (json/read-json
     (:body
      (http/get (compose-request-url
                 "https://app.grouptexting.com/api/credits/check/"
                 {}))))))
	       
