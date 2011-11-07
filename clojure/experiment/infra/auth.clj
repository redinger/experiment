(ns experiment.infra.auth
  (:use somnium.congomongo)
  (:require [experiment.infra.session :as session]
	    [clojure.string :as str]
	    [noir.util.crypt :as crypt]))
;;
;; Authentication
;;

(defn encrypt-password
  [salt plaintext]
  (crypt/encrypt plaintext))

(defn compare-passwords
  [encrypted salt plaintext]
  (crypt/compare plaintext encrypted))

(defn generate-salt []
  (.substring (.toString (java.util.UUID/randomUUID)) 0 8))


(defn set-user-password
  "Set the salt and encrypt the user's plaintext password"
  [user plaintext]
  (let [salt (generate-salt)]
    (assoc user
      :salt salt
      :password (encrypt-password plaintext salt))))

;;
;; Session Authentication and Common State
;;
;; NOTE: Models are not defined yet, so we'll special case
;; support for the current-user

(defn login
  "Due to the use of middleware to track the user, we need to
   ensure that any handler redirects after log-ins"
  [auth]
  (let [user (or (fetch-one :users :where {:username (:username auth)})
		 (fetch-one :users :where {:email (:username auth)}))
	encrypted (and user (:password user))]
    (if (and encrypted (crypt/compare encrypted (:password auth)))
      (do (session/clear!)
	  (session/put! :logged-in? true)
	  (session/put! :userid (:_id user))
	  user)
      nil)))

(defn logout []
  (session/clear!))

