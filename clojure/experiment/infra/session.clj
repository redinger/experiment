(ns experiment.infra.session
  (:use ring.middleware.session.store
	somnium.congomongo)
  (:refer-clojure :exclude [get])
  (:require
   [somnium.congomongo.coerce :as coerce]
   [noir.session :as session]
   [noir.util.crypt :as crypt])
  (:import java.util.UUID))

;;
;; Use Mongo for our Session Store
;;

(deftype MongoSessionStore [coll]
  SessionStore
  (read-session [_ key]
    (binding [coerce/*keywordize* false]
      (dissoc (fetch-one coll :where {:*session-key* key})
	      "*session-key*" "_id")))
  (write-session [_ key data]
    (let [key (or key (str (UUID/randomUUID)))]
      (fetch-and-modify coll {:*session-key* key} (assoc data :*session-key* key)
			:upsert? true)
      key))
  (delete-session [_ key]
    (destroy! coll {:*session-key* key})
    nil))

(defn mongo-session-store
  "Creates a mongo-backed session storage engine."
  ([] (mongo-session-store :sessions))
  ([coll] (MongoSessionStore. coll)))

;;
;; Session event counters
;;

(defn inc! [key & [num]]
  (let [old (session/get key)
	new (+ old (or num 1))]
    (session/put! key new)
    new))

(def put! session/put!)
(def get session/get)
(def remove! session/remove!)
(def clear! session/clear!)
(def flash-put! session/flash-put!)
(def flash-get session/flash-get)

;;
;; Session Authentication and Common State
;;

(defn user []
  (try 
    (and (session/get :logged-in?)
	 (fetch-one :users :where {:_id (session/get :user)}))
    (catch java.lang.Throwable e
      (println "Get User from Session Error: " e)
      nil)))
	
(defn login [user]
  (let [record (or (fetch-one :users :where {:username (:username user)})
		   (fetch-one :users :where {:email (:username user)}))]
    (if (and record (crypt/compare (:password user) (:password record)))
      (do (session/put! :logged-in? true)
	  (session/put! :user (:_id record))
	  user)
      nil)))

(defn logout []
  (session/put! :logged-in? false))

(defn logged-in? []
  (and (session/get :logged-in?)
       (user)))
       


