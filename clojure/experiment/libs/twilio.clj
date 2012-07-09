(ns experiment.libs.twilio
  (:use
   noir.core
   experiment.infra.models)
  (:refer-clojure :exclude [get])
  (:require
   [clojure.string :as string]
   [clojure.tools.logging :as log]
   [clj-http.client :as http]
   [cheshire.core :as json]
   [clj-time.core :as time]
   [experiment.libs.datetime :as dt]
   [experiment.libs.properties :as props]
   [somnium.congomongo :as mongo]
   [noir.response :as response]
   [noir.request :as request]))

;;
;; Partial Twilio API Support
;;

;;
;; Utils
;;

(defn camelcase->dashed
  "GetThisValue -> get-this-value"
  ([string]
     (camelcase->dashed string "-"))
  ([string sep]
     (->> (string/split string #"(?=[A-Z])")
          (filter #(> (count %) 0))
          (map string/lower-case)
          (string/join sep))))

(defn dashed->camelcase [#^String string]
  "get-this-value -> getThisValue"
  (let [[head & rest] (string/split string #"[-_]")]
    (string/join (cons head (map string/capitalize rest)))))

(defn dashed->pascalcase
  "get-this-value -> GetThisValue"
  [string]
  (->> (string/split string #"[-_]")
       (map string/capitalize)
       string/join))

(defn pascalcase-map [themap]
  (zipmap (map (comp dashed->pascalcase name) (keys themap))
          (vals themap)))

(defn underscorecase-map [themap]
  (zipmap (map (comp keyword #(camelcase->dashed % "_") name) (keys themap))
          (vals themap)))

;;
;; Twilio REST APIs
;;

(def ^{:private true} api-base  "https://api.twilio.com/2010-04-01")
(defonce ^{:private true} account (atom nil))

(defn- as-auth [acct]
  [(:sid acct) (:auth_token acct)])
  
(defn- with-auth [request]
  (assoc request :basic-auth (as-auth @account)))

(defn- with-debug [request]
  (assoc request :throw-exceptions false))

(defn- as-json [request]
  (assoc request :content-type :json))

(defn- postfix-json [url]
  (str url ".json"))

(defn- handle-reply [request]
  (let [status (:status request)]
    (if (and (>= status 200)
             (< status 300))
      (json/parse-string (:body request) true)
      (do (println request)
          (java.lang.Error. "Invalid response")))))

(defn- get-command
  ([url request]
     (assert @account)
     (->> request
          with-debug
          with-auth
          as-json
          (http/get (postfix-json url))
          handle-reply))
  ([url]
     (get-command url {})))

(defn- post-command
  ([url request]
     (assert @account)
     (println url request)
     (->> request
          with-debug
          with-auth
          (http/post (postfix-json url))
          handle-reply))
  ([url]
     (post-command url {})))

(defn- put-command
  ([url request]
     (assert @account)
     (->> request
          with-debug
          with-auth
          (http/put (postfix-json url))
          handle-reply))
  ([url]
     (put-command url {})))

(defn- delete-command
  ([url request]
     (assert @account)
     (->> request
          with-debug
          with-auth
          (http/delete (postfix-json url))
          handle-reply))
  ([url]
     (delete-command url {})))

;;
;; Resource protocols
;;

;; Twilio exposes a restful resource API
(defprotocol RESTResource
  "A protocol for getting and updating URL resource properties.
   Commands return a fresh object of the type including side-effects."
  (url [res] "The URL of the resource")
  (get [res] "Merge server properties into object")
  (post! [res] "Create/update resource on the server")
  (put! [res] [res props] "Update resource properties")
  (delete! [res] "Remove resource from server"))

;; Some Twilio URLs expose generic find or create methods
(defn resources-dispatch [& args]
  (:type (first args)))
(defmulti create-resource resources-dispatch)
(defmulti list-resources resources-dispatch)

(defn not-supported
  "Function to call for RESTResource methods that
   are not supported by the endpoint"
  ([]
     (throw (java.lang.Error. "Not supported")))
  ([msg]
     (throw (java.lang.Error. msg))))

;;
;; ## Account operations
;;

(defn- account-params [acct]
  (pascalcase-map
   (select-keys acct [:friendly_name :status])))

(defrecord Account [sid auth_token]
  RESTResource
  (url [acct] (format "%s/Accounts/%s" api-base (:sid acct)))
  (get [acct] (merge acct (get-command (url acct))))
  (put! [acct params]
    (map->Account
     (post-command (url acct)
                   {:form-params params})))
  (put! [acct] (put! account (account-params acct)))
  (post! [acct] (put! acct))
  (delete! [acct] (not-supported "Cannot delete accounts")))

(defmethod print-method Account [acct writer]
  (.write writer (format "#Account[\"%s\"]" (:friendly_name acct))))

(defn set-account!
  "Initialize the account variable with new sid and token
   then ensure it has been updated by the server"
  ([sid token]
     (swap! account (fn [old] (Account. sid token)))
     (swap! account get))
  ([]
     (set-account!
      (props/get :twilio.sid)
      (props/get :twilio.token))))

(defn set-account-status
  "Change an Account's status"
  [account status]
  (put! account {:status status}))

(defn set-account-name
  "Change an Account's friendly name"
  [account name]
  (put! account {:friendly_name name}))

(defn account-proxy
  "Creates a proxy object using only the sid"
  [sid]
  {:sid sid})


;;
;; ## Subaccounts
;;

(def Accounts {:type :subaccounts :url (str api-base "/Accounts")})

(defmethod create-resource :subaccounts [factory name]
  (map->Account
   (post-command (:url factory) {:FriendlyName name})))

(defn- master-account? [account]
  (= (:owner_account_sid account) (:sid account)))

(defn- subaccount? [account]
  (not (master-account? account)))

(defmethod list-resources :subaccounts [factory]
  (->> (:url factory)
       get-command 
       :accounts
       (map map->Account)
       (filter subaccount?)))
               
(defn get-subaccount [name]
  (->>
   (list-resources Accounts)
   (filter #(= (:friendly_name %) name))
   first))
           
(defn close-account [account]
  (set-account-status account "closed"))



;;
;; Applications
;;

(declare map->Application)

(defrecord Application [sid account_sid]
  RESTResource
  (url [app] (format "%s/Accounts/%s/Applications/%s"
                     api-base (:account_sid app) (:sid app) ))
  (get [app] (get-command (url app)))
  (post! [app] (map->Application
                (post-command (url app)
                              {:form-params
                               (pascalcase-map
                                (dissoc app :sid :account_sid))})))
  (put! [app] (post! app))
  (put! [app params] (post! (merge app params)))
  (delete! [app] (delete-command (url app))))

(defmethod print-method Application [app writer]
  (.write writer (format "#Application[\"%s\"]" (:friendly_name app))))
  

(def ApplicationList
  {:type :applications
   :urlfn (fn [account]
            (format "%s/%s/Applications" (:url Accounts) (:sid account)))})

(defmethod list-resources :applications [factory account]
  (->> ((:urlfn factory) account)
       get-command 
       :applications
       (map map->Application)))

(defmethod create-resource :applications [factory account params]
  (map->Application
   (post-command ((:urlfn factory) account)
                 {:form-params
                  (pascalcase-map params)})))

(defn make-site-sms-app [account name url fallback-url]
  (create-resource ApplicationList account
                   {:friendly-name name
                    :sms-url url
                    :sms-fallback-url fallback-url}))

(defn applications [account]
  (list-resources ApplicationList account))

(defn get-application [account name]
  (->>
   (list-resources ApplicationList account)
   (filter #(= (:friendly_name %) name))
   first))

(comment
  ;; Default personal experiments application
  (make-site-sms-app account
                     "PersonalExperiments SMS Application"
                     "http://www.personalexperiments.org/sms/submit"
                     "http://www.personalexperiments.org/sms/error"))



;;
;; Manage phone numbers
;;

(defrecord AvailableNumber [phone_number iso_country friendly_name])

(defmethod print-method AvailableNumber [anum writer]
  (.write writer (format "#AvailableNumber[%s]" (:friendly_name anum))))

(def AvailableNumbers {:type :available
                       :urlfn (fn [country]
                                (format "%s/%s/AvailablePhoneNumbers/%s/Local"
                                        (:url Accounts)
                                        (:sid @account) country))})

;; Takes arguments: area-code, contains, in-region, in-postal-code
(defmethod list-resources :available [factory country &
                                      {:keys [area-code contains in-region in-postal-code] :as options}]
  (map map->AvailableNumber
       (:available_phone_numbers
        (get-command
         ((:urlfn factory) country)
         {:query-params (pascalcase-map options)}))))

(def PhoneNumbers
  {:type :incoming
   :urlfn (fn [account]
            (str (url account) "/IncomingPhoneNumbers"))})

(defrecord PhoneNumber [sid account_sid friendly_name phone_number sms_application_sid]
  RESTResource
  (url [number]
    (str api-base "/Accounts/" (:account_sid number) "/IncomingPhoneNumbers/"
         (:sid number)))
  (get [number] (merge number (get-command (url number))))
  (put! [number map]
    (post! (merge number map)))
  (put! [number]
    (post! number))
  (post! [number]
    (post-command (url number)
                  {:form-params
                   (pascalcase-map
                    (dissoc number :sid :account_sid))}))
  (delete! [number]
    (delete-command (url number))))

(defmethod print-method PhoneNumber [num writer]
  (.write writer (format "#PhoneNumber[%s]" (:phone_number num))))

;; Return a list of the accounts phone numbers
(defmethod list-resources :incoming [factory account]
  (map map->PhoneNumber
       (:incoming_phone_numbers
        (get-command ((:urlfn factory) account)))))

(defmethod create-resource :incoming
  [factory account anumber params]
  (assert (= (type account) Account)
          (or (number? anumber) (= (type anumber) AvailableNumber)))
  (map->PhoneNumber
   (post-command ((:urlfn PhoneNumbers) account)
                 {:form-params
                  (pascalcase-map
                   (merge (if (number? anumber)
                            {:area-code anumber}
                            (select-keys anumber :phone_number))
                          params))})))

(defn numbers
  ([account]
     (list-resources PhoneNumbers account))
  ([]
     (list-resources PhoneNumbers @account)))

(defn set-application! [num application]
  (assert (= (type num) PhoneNumber))
  (map->PhoneNumber
   (post! (assoc num :sms_application_sid (:sid application)))))

(comment
  ;; Set the master account
  ;;   pulls from site property file by default
  (set-account!) 
  ;; Get the first number in any subaccount
  (first (numbers (first (subaccounts))))
  ;; Get the first number in a specific subaccount
  (first (numbers (get-subaccount "PersonalExperiments")))
  ;; Define a specific test account, number and application
  (def test-account (get-subaccount "PersonalExperiments"))
  (def test-number (first (numbers test-account)))
  (def test-application (get-application test-account "PersonalExperiments SMS Application"))
  ;; Ensure application is set
  (set-application! test-number test-application))

;;
;; SMS Messages
;;

(declare map->SMSMessage)

(defrecord SMSMessage [sid account_sid date_created from to body status direction uri api_version price]
  RESTResource
  (url [msg] (str ((:urlfn SMSMessages) {:sid (:account_sid msg)})
                  "/"
                  (:sid msg)))
  (get [msg] (map->SMSMessage (get-command (url msg))))
  (post! [msg] (not-supported))
  (put! [msg] (not-supported))
  (put! [msg params] (not-supported))
  (delete! [msg] (not-supported)))

(defmethod print-method SMSMessage [msg writer]
  (.write writer (format "#SMSMessage[from %s on %s]" (:from msg) (:date_sent msg))))

(def SMSMessages
  {:type :messages
   :urlfn (fn [account]
            (format "%s/%s/SMS/Messages" (:url Accounts) (:sid account)))})

(defmethod list-resources :messages [factory account &
                                     {:keys [to from date-sent] :as options}]
  (->> (get-command ((:urlfn factory) account)
                    {:query-params (pascalcase-map options)})
       :sms_messages
       (map map->SMSMessage)))

(defmethod create-resource :messages [factory account message]
  (assert (= 3 (count (filter #{:from :to :body} (keys message)))))
  (post-command ((:urlfn factory) account)
                {:form-params (pascalcase-map message)}))

(defn send-sms [number to message]
  (assert (= (type number) PhoneNumber))
  (create-resource SMSMessages
                   (account-proxy (:account_sid number))
                   {:from (:phone_number number)
                    :to to
                    :body message}))

(comment
  ;; Assumes defs from comment above
  ;; Ian's phone
  (def test-dest "+16172858787") 
  (send-sms test-number test-dest "This is a test"))

;;
;; ## SMS Receive URL and Handlers
;;

(defonce handlers (atom {}))

(defn register-sms-handler
  "Register a handler, name optional if you want multiple handlers"
  ([name handler]
     (swap! handlers assoc name handler)
     name)
  ([handler]
     (register-sms-handler :default handler)))

(defn unregister-sms-handler
  ([name]
     (swap! handlers dissoc name))
  ([]
     (unregister-sms-handler :default)))

(defn receive-sms-handler [message]
  (doseq [handler (vals @handlers)]
    (handler message)))

(defpage receive-sms [:post "/sms/submit"]
  {:as args}
  (println "Received SMS")
  (receive-sms-handler (map->SMSMessage (underscorecase-map args)))
  (response/empty))

;;
;; ## SMS Error Callback
;;

(defonce error-handling (atom nil))

(defn register-error-handler [handler]
  (swap! error-handling (fn [old] handler))
  true)

(defn receive-sms-error [args]
  (when-let [handler @error-handling]
    (handler args)))

(defpage receive-sms-error [:post "/sms/error"]
  {:as args}
  (println "Received Error")
  (receive-sms-error (underscorecase-map args)))


;;
;; Debugging
;; --------------------------

(def Notifications
  {:type :notifications
   :urlfn (fn [account]
            (format "%s/%s/Notifications" (:url Accounts) (:sid account)))})

(defmethod list-resources :notifications [factory account &
                                          {:keys [log message-date]
                                           :as options}]
  (get-command ((:urlfn factory) account)
               (pascalcase-map options)))

;;
;; Test Receive
;;

(defonce mailbox (agent {}))

(defn add-message [inbox message]
  (assoc inbox (:sid message) message))

(defn add-to-mailbox [message]
  (send-off mailbox add-message message))
  
(defpage test-mailbox-post [:post "/sms/test/receive"]
  {:as args}
  (println args)
  (add-to-mailbox (map->SMSMessage (underscorecase-map args)))
  (response/empty))


