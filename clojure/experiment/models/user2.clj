(ns experiment.models.user2
  (:require [experiment.infra.auth :as auth])
  (:use experiment.infra.datomic
        [datomic.api :only (db q) :as d]))

(defn create-user! [username name password email]
  (create-model!
   (auth/set-user-password
    {:type :user
     :username username
     :uname (.toLowerCase username)
     :name name
     :email email
;;     :services {}
;;     :trackers {}
;;     :trials {}
;;     :preferences {}
;;     :journals {}
     }
    password)))

(comment
  (def test-user
    {:_id "q325235235233236236236"
     :type "user"
     :username "eslick"})

  @(d/transact
    conn
    [{:db/id (d/tempid :db.part/user)
      :user/id "q1=2345"
      :user/username "eslick"}])


)