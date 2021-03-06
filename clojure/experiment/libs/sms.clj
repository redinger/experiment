(ns experiment.libs.sms
  (:use
   noir.core
   experiment.infra.models)
  (:require
   [clojure.string :as string]
   [clojure.tools.logging :as log]
   [clj-http.client :as http]
   [cheshire.core :as json]
   [clj-time.core :as time]
   [experiment.libs.datetime :as dt]
   [somnium.congomongo :as mongo]
   [noir.response :as response]
   [noir.request :as request]))

;;
;; A simple SMS gateway based on grouptexting API
;; -----------------------------------------------


;; ## Manage GroupTexting Credentials

(defonce ^{:dynamic true}
  *credentials* nil)

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

;; ## Make a request to the grouptexting API

(defn- compose-request-url
  "Compose a map of args as key-value pairs on the base URL"
  [base argmap]
  (log/spy
   (apply str base
	  "?"
	  (http/generate-query-string
	   (inject-perms argmap)))))

;;
;; ## Log all the messages we get
;;
;; Log all the messages we get to Mongo for forensic purposes

(defn log-message! [message]
  (assert (and (:from message) (:message message)))
  (mongo/insert! :sms (assoc message
                        :date (dt/as-date (dt/now))
                        :type :sms)))

(defn get-messages
  ([from]
     (mongo/fetch :sms :where {:from from} :sort {:date 1} :limit 20))
  ([start end]
     (mongo/fetch :sms :where {:date {:$gte (dt/as-date start) :$lte (dt/as-date end)}}
                  :sort {:date 1}))
  ([from start end]
     (mongo/fetch :sms :where {:from from :date {:$gte (dt/as-date start) :$lte (dt/as-date end)}} :sort {:date 1}))

  ([from start end prefix]
     (mongo/fetch :sms :where {:from from :date {:$gte (dt/as-date start) :$lte (dt/as-date end)} :message (re-pattern (str "(?i)^" prefix))} :sort {:date 1})))
     

(defn get-latest-message [from]
  (first
   (mongo/fetch :sms :where {:from from}
                :sort {:date -1}
                :limit 1)))

;;
;; SMS API
;; ------------------------------

;; ## Send an SMS

(defn- clean-phone-number [number]
  (string/replace number #"-" ""))

(defn send-sms [number message & [credentials]]
  (assert (< (count message) 160))
  (assert (not (re-find #"[']" message)))
  (with-credentials [credentials]
    (http/get (compose-request-url
	       "https://app.grouptexting.com/api/sending"
	       {:phonenumber (clean-phone-number number)
            :message message}))))

;; ## Account mgmt

(defn account-balance [& [credentials]]
  (with-credentials [credentials]
    (json/parse-string
     (:body
      (http/get (compose-request-url
                 "https://app.grouptexting.com/api/credits/check/"
                 {})))
     true)))

;;
;; SMS Inbox Handler
;; -----------------------------------

(defn default-handler [from message]
  (log/spy [from message]))

(defonce ^{:dynamic true} *handler* 'default-handler)

(defn set-reply-handler [handler]
  (alter-var-root #'*handler* (fn [old] handler)))

(defn apply-reply-handler [& args]
  (apply (var-get #'*handler*) args))

(defn reply-handler? []
  (var-get #'*handler*))

(defn- handle-reply
  "Internal handler for any received SMS messages.
   Logs the message then calls the user handler if defined.
   from and message are strings"
  [from message]
  (let [ts (dt/now)]
    (log-message! {:from from :message message :ts (dt/as-date ts)})
    (when (reply-handler?)
      (apply-reply-handler ts from message))))

(defn replay-logged-message [record]
  (let [{:keys [ts from message]} record]
    (when (reply-handler?)
      (apply-reply-handler ts from message))))

(defpage inbox-url [:get "/sms/receive"]
  {:keys [from message] :as args}
  (handle-reply from message)
  (response/empty))


;;
;; API: Parse SMS Content
;; ----------------------------------------
;;
;; Parse and associate replies with events, submit data.
;; The idea is that we look at active events for a user,
;; and see if any of them are compatible with the data
;; we're seeing in the SMS message.

(defmulti parse-sms
  "A multi-method that parses an SMS response according to
   the :sms-parser type specified in the event.  Override
   to do something other than the prefix default.  Timestamp
   returned is the receipt timestamp; events may need to
   adjust the reference timestamp to submit this as a valid
   sample object."
  (fn [message ts event]
    (when-let [name (:sms-parser event)]
      (keyword name))))

(def patterns (atom nil))

(defn default-sms-parser-re
  "Return an RE pattern that recognized prefix + data type.
   Memoizes patterns; silly performance optimization..."
  [event]
  (let [{:keys [sms-prefix sms-value-type]} event
        lookup [sms-prefix sms-value-type]]
    (or (find @patterns lookup)
        (swap! patterns assoc lookup
               (re-pattern
                (str (or (str "(?i)" (:sms-prefix event)) "")
                     (case (:sms-value-type event)
                       "string" "\\s*([^\\s]+)"
                       "float" "\\s*([\\d\\.]+)"
                       "\\s*(\\d*)")))))))

(defn default-sms-parser
  "This is the default SMS parser, it supports simple
   SMS formats like 'mood good' or 'stool 4' or just 's 10'

   - :sms-parser :default | nil
   - :sms-prefix '<SMS prefix>'
   - :sms-value-type '<type of value after prefix>' | nil <int by default>
  "  
  [message event]
  (when-let [value (second (re-matches (second (default-sms-parser-re event)) message))]
    (case (:sms-value-type event)
      "string" (if-let [dom (:sms-domain event)]
                 (if-let [result (dom (keyword value))]
                   result
                   (throw (java.lang.Error. "Invalid value")))
                 value)
      "float" (Float/parseFloat value)
      (Integer/parseInt value))))

(defn recode-val [event val]
  (if-let [mapr (:sms-domain event)]
    (mapr (if (string? val) (keyword val) val))
    val))

(defmethod parse-sms :default [message ts event]
  (try 
    (when-let [val (and (:sms-prefix event)
                        (default-sms-parser message event))]
      {:ts ts :v (recode-val event val) :raw message :event event})
    (catch java.lang.Error e
      nil)))


       
