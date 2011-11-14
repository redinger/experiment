(ns experiment.infra.auth
  (:use somnium.congomongo)
  (:require [experiment.infra.session :as session]
	    [clojure.string :as str]
	    [noir.util.crypt :as crypt]))
;;
;; Authentication
;;

(defn set-user-password
  "Set the salt and encrypt the user's plaintext password"
  [user plaintext]
  (assoc user
    :password (crypt/encrypt plaintext)))

;;
;; Session Authentication and Common State
;;
;; NOTE: Models are not defined yet, so we'll special case
;; support for the current-user

(defn login
  "Due to the use of middleware to track the user, we need to
   ensure that any handler redirects after log-ins"
  [auth]
  (let [user (or (fetch-one :user :where {:username (:username auth)})
		 (fetch-one :user :where {:email (:username auth)}))
	encrypted (and user (:password user))]
    (if (and encrypted (crypt/compare (:password auth) (:password user)))
      (do (session/clear!)
	  (session/put! :logged-in? true)
	  (session/put! :userid (:_id user))
	  user)
      nil)))

(defn logout []
  (session/clear!))

