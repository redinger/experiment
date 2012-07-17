(ns experiment.models.user2
  (:require [experiment.infra.session :as session]
            [experiment.infra.middleware :as mid]
            [experiment.infra.auth :as auth])
  (:use noir.core
        experiment.infra.datomic))

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
     :uname (.toLowerCase username)
     :name name
     :email email
     :services {}
     :trackers {}
     :trials {}
     :preferences {}
     :journals {}}
    password)))
    
(defn get-user
  "Model for reference"
  [reference]
  (if (string? reference)
    (or (fetch-model :user {:uname (.toLowerCase reference)})
        (fetch-model :user {:email reference}))
    reference))

(mid/set-user-fetcher
 (fn [& {:keys [id username email]}]
   (cond id (fetch-model :user {:_id id})
         username (get-user username)
         email (get-user email))))

(defmethod public-keys :user [user]
  (if (= (:_id user) (:_id (session/current-user)))
    (keys (apply dissoc user
                 [:updates :permissions :password :salt :dataid :state]))
    [:username :bio :name]))

;; ## Trials

(defn attach-user [user submodel]
  (assoc submodel :user user))

(defn trials [user]
  (map (partial attach-user user) (vals (:trials user))))

(defn get-trial [user id]
  (attach-user user ((:trials user) (keyword id))))

(defn has-trials? [user]
  (if (not (empty? (:trials user))) true false))

;; ## Trackers

(defn add-tracker! [user instrument schedule]
  (let [inst (if (model? instrument)
               instrument
               (fetch-model :instrument {:src instrument}))
        submod {:type "tracker"
                :state "active"
                :user user
                :instrument inst
                :schedule schedule}]
    (create-model! submod)))

(defn trackers [user]
  (vals (:trackers user)))

(defn has-trackers? [user]
  (if (not (empty? (trackers user))) true false))

(defn remove-tracker! [user tracker]
  (delete-model! (:id tracker)))

;; ## Services

(defn set-service [user service record]
  {:pre [(model? user)
         (model? record)]}
  (do
    ;; create transaction to:
    ;; 1. If an a service with this ID already exists:
    ;;    - retract association of user with existing service
    ;;    - retract existing service
    ;; 2. create a new service entity
    ;; 3. assert relation between user and new service
    )
  )

(defn set-service-param [user service param value]
  (assert (model? user))
  (modify-model! user {:$set {:services {param value}}}))

(defn get-service-param [user service entry]
  (assert (model? user))
  (get-in user [:services service entry]))


;; ## User Properties

(defn get-pref
  ([user property]
     (get-in user [:preferences property]))
  ([property]
     (get-pref (session/current-user) property)))
  
(defn set-pref!
  ([user property value]
     (modify-model! user {:$set {:preferences {property value}}}))
  ([property value]
     (set-pref! (session/current-user) property value)))

;; ## User Permissions

(defn set-permission!
  ([user perm value]
     (modify-model! user {:$set {:permissions {perm value}}}))
  ([perm value]
     (set-permission! (session/current-user) perm)))

(defn has-permission? [perm]
  ((set (:permissions (session/current-user))) perm))

(defn is-admin? []
  (has-permission? "admin"))
  
(defn site-admins []
  (list (get-user "eslick")))

(defn site-admin-refs []
  (map as-dbref (site-admins)))
  
;; Generate Test Users
;; ------------------------

(defn gen-first [] (rand-nth ["Joe" "Larry" "Curly" "Mo"]))
(defn gen-last [] (rand-nth ["Smith" "Carvey" "Kolluri:" "Kramlich"]))
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


