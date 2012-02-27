(ns experiment.infra.session
  (:use ring.middleware.session.store
	somnium.congomongo)
  (:refer-clojure :exclude [get])
  (:require
   [experiment.infra.middleware :as mid]
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

;;
;; Proxy session commands
;;

(def put! session/put!)
(def get session/get)
(def remove! session/remove!)
(def clear! session/clear!)
(def flash-put! session/flash-put!)
(def flash-get session/flash-get)

;;
;; The session user and logged-in status
;;
;; NOTE: A hack to get around the fact that models aren't defined
;; yet in the infra directory

(defn active? []
  (not (= (type (noir.request/ring-request)) clojure.lang.Var$Unbound)))

(defn current-user []
  (try 
    (if (active?)
      (and (get :logged-in?)
	   mid/*current-user*)
      (fetch-one :user :where {:username "eslick"}))
    (catch java.lang.Throwable e
      (clojure.tools.logging/error "Get User from Session Error: " e)
      nil)))
	
(defn logged-in? []
  (when (current-user) true))
       


