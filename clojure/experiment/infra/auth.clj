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

(defn valid-password? [user plaintext]
  (and user (crypt/compare plaintext (:password user))))

(defn lookup-user-for-auth [id]
  (or (fetch-one :user :where {:username id})
      (fetch-one :user :where {:email id})))

(defn login
  "Due to the use of middleware to track the user, we need to
   ensure that any handler redirects after log-ins"
  [auth]
  (let [user (lookup-user-for-auth (:username auth))]
    (if (valid-password? user (:password auth))
      (do (session/clear!)
          (session/put! :logged-in? true)
          (session/put! :userid (:_id user))
          user)
      nil)))

(defn logout []
  (session/clear!))

