(ns experiment.models.user
  (:use noir.core
	experiment.infra.models)
  (:require [experiment.infra.session :as session]
	    [experiment.infra.auth :as auth]))

;;(defmodel user 
;;  :collection :users
;;  :dispatch [:cname "users" :valid-options ["q" "page" "size"]]
;;  :validation valid-user?
;;  :client-fields [:name :avatar :bio :gender :country :state
;;		  :weight :yob :units :default_privacy
;;		  :background :acl]
;;  :embedded-models [:background background :acl acl])

;; Server Models:
;; - bb dispatch
;; - Handler for options such as search, pagination, etc
;; - Validation on creation, update, etc.

;; USER
;; ===========================
;; {
;;  :name "Full Name"
;;  :avatar <image>
;;  :bio
 
;;  Demographics
;;  :gender
;;  :country
;;  :state

;;  Physical
;;  :weight
;;  :dob

;;  Preferences
;;  :units
;;  :default_privacy

;;  Collections
;;  :background
;;  :acl
;;  }
;; ===========================
 

;;
;; Users
;;

(defmethod valid-model-params? :user [user]
  (and (:username user)))

(defn has-permission? [perm]
  ((set (:permissions (session/current-user))) perm))

(defn is-admin? []
  (has-permission? "admin"))
  
;;(defmethod client-keys :user [user]
;;  [:name :avatar :bio :gender :country :state
;;   :weight :yob :units :default_privacy
;;   :background :acl])

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
	(fetch-model :user :where {:username reference})
	true
	(resolve-dbref reference)))

(defn get-user-dbref [reference]
  (cond (and (map? reference) (= (name (:type reference)) "user"))
	(as-dbref reference)
	true
	(as-dbref (get-user reference))))
     

;;
;; Test Users
;;

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

;;(defn gen-users [count]
;;  (dotimes [i count]
;    (create-model! (gen-user))))


