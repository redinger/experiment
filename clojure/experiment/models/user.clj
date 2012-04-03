(ns experiment.models.user
  (:use noir.core
	experiment.infra.models)
  (:require [experiment.infra.session :as session]
	    [experiment.infra.auth :as auth]))

;; USER
;; ---------------------------------
;;  :username (client, :public)
;;  :name "Full Name" (client, :owns|:friend)
;;  :email "Account email" (client, :owns)
;;  :password (server)
;;  :avatar <image> (client, :public)
;; 
;;  Demographics (:demog, client, :owns)
;;    :age
;;    :gender
;;    :country
;;    :state
;;    :weight
;;    :height
;;
;;  Profile (:profile, client, :owns)
;;    :bio
;;    :units
;;    :default_privacy
;;    :cell
;;
;;  Permissions (:perm, server)
;;    ...
;;
;;  :trackers []
;;  :trials []
;;  :past_trials []

;; ## Convenience methods

(defmethod valid-model? :user [user]
  (and (:username user)
       (:email user)
       (:type user)))

(defn create-user! [username password email name]
  (create-model!
   (auth/set-user-password
    {:type :user
     :username username
     :name name
     :email email}
    password)))
    
(defn get-user
  "Model for reference"
  [reference]
  (cond (string? reference)
	(or (fetch-model :user {:username reference})
            (fetch-model :user {:email reference}))
	true
	(resolve-dbref reference)))

(defn get-user-dbref [reference]
  (cond (and (map? reference) (= (name (:type reference)) "user"))
	(as-dbref reference)
	true
	(as-dbref (get-user reference))))

(defmethod public-keys :user [user]
  (keys (apply dissoc user
               [:updates :permissions :password :salt :dataid :state])))

;; ## Trials

(defn trials [user]
  (vals (:trials user)))

(defn has-trials? [user]
  (if (not (empty? (:trials user))) true false))

;; ## Trackers

(defn add-tracker! [user instrument schedule]
  (create-submodel!
   user
   {:type "tracker"
    :instrument (as-dbref instrument)
    :schedule schedule}))

(defn trackers [user]
  (vals (:trakers user)))

(defn has-trackers? [user]
  (if (not (empty? (trackers user))) true false))

(defn remove-tracker! [user tracker]
  (delete-submodel! user tracker))

;; ## Services

(defn set-service [user service record]
  (assert (model? user))
  (modify-model! user {:$set {:services {service record}}}))

(defn set-service-param [user service param value]
  (assert (model? user))
  (modify-model! user {:$set {:services {param value}}}))

(defn get-service-param [user service entry]
  (assert (model? user))
  (get-in user [:services service entry]))


;; ## User Properties

(defn get-pref
  ([user property]
     (get-in user [:prefs property]))
  ([property]
     (get-pref (session/current-user) property)))
  
(defn set-pref!
  ([user property value]
     (modify-model! user {:$set {:prefs {property value}}}))
  ([property value]
     (set-pref! (session/current-user) property value)))

;; ## User Permissions

(defn has-permission? [perm]
  ((set (:permissions (session/current-user))) perm))

(defn is-admin? []
  (has-permission? "admin"))
  


;; Generate Test Users
;; ------------------------

(defn gen-first [] (rand-nth ["Joe" "Larry" "Curly" "Mo"]))
(defn gen-last [] (rand-nth ["Smith" "Carvey" "Kolluri" "Kramlich"]))
(defn gen-gender [] (rand-nth ["M" "F"]))
(defn gen-weight [] (+ 100 (rand-int 150)))
(defn gen-yob [] (+ 1940 (rand-int 54)))

(defn gen-user []
  {:type :user
   :name (str (gen-first) " " (gen-last))
   :bio "I have no bio, I am a computer generated character"
   :gender (gen-gender)
   :country "USA"
   :state "CA"
   :weight (gen-weight)
   :yob (gen-yob)})

(defn gen-users [count]
  (dotimes [i count]
    (create-model! (gen-user))))


