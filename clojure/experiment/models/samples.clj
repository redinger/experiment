(ns experiment.models.samples
  (:use experiment.infra.models)
  (:require [clojure.java.jdbc :as sql]
	    [clojureql.core :as cql]
	    [experiment.libs.datetime :as dt]
	    [clj-time.core :as time]))

;; DESCRIPTION:
;; - Store datasets for instrument trackers

;;
;; Schema and helpers
;;

(def db {:classname "com.mysql.jdbc.Driver"
	 :subprotocol "mysql"
	 :user "client"
	 :password "trackme"
	 :subname "//localhost:3306/experiment"})

(defn create-tables []
  (sql/with-connection db
    (sql/create-table
     :instruments
     [:id :int "PRIMARY KEY" "AUTO_INCREMENT"]
     [:name "varchar(256)"]
     [:iid "varchar(256)"])
    (sql/create-table
     :users
     [:id :int "PRIMARY KEY" "AUTO_INCREMENT"]
     [:username "varchar(40)"]
     [:uid "varchar(256)"])
    (sql/create-table
     :samples
     [:user :int "NOT NULL"]
     [:inst :int "NOT NULL"]
     [:ts :bigint "NOT NULL"]
     [:value :double])
    (sql/do-commands
     "CREATE UNIQUE INDEX samples_idx ON samples (user,inst,ts)")))

(defn drop-tables []
  (sql/with-connection db
    (doall (map sql/drop-table [:users :instruments :samples]))))

(def samples (cql/table db :samples))

(def users (cql/table db :users))
(defn user [uid]
  (cql/select users (cql/where (= :uid uid))))
(defn user-id [uid]
  (cql/pick (user uid) :id))

(def instruments (cql/table db :instruments))
(defn instrument [iid]
  (cql/select instruments (cql/where (= :iid iid))))
(defn instrument-id [iid]
  (cql/pick (instrument iid) :id))

(defn inst-samples [user inst]
  (-> samples
      (cql/select (cql/where (= :samples.user user)))
      (cql/select (cql/where (= :samples.inst inst)))))

;;
;; Manage identity
;;

(defn id [model]
  (str (:_id model)))

(defn clear-identity-cache []
  (unset-collection-field :user :dataid)
  (unset-collection-field :instrument :dataid))

(defn as-sql-user [model]
  (if-let [id (:dataid model)]
    id
    (let [dbid @(user-id (id model))]
      (if dbid
	(do (update-model! (assoc model :dataid dbid))
	    dbid)
	(do @(cql/conj! users {:username (:username model)
			       :uid (id model)})
	    (as-sql-user model))))))

(defn as-sql-instrument [model]
  (if-let [id (:dataid model)]
    id
    (let [dbid @(instrument-id (id model))]
      (if dbid
	(do (update-model! (assoc model :dataid dbid))
	    dbid)
	(do @(cql/conj! instruments {:name (:name model)
				     :iid (id model)})
	    (as-sql-instrument model))))))

;;
;; Get sample series
;;

(defn get-samples
  ([user instrument]
     (get-samples user instrument 0 (dt/now)))
  ([user instrument start end]
     (let [start (dt/as-utc start)
	   end (dt/as-utc end)]
       (map (comp vec vals)
	    @(-> (inst-samples (as-sql-user user) (as-sql-instrument instrument))
		 (cql/select (cql/where (and (>= :ts start) (<= :ts end))))
		 (cql/project [:ts :value])
		 (cql/sort [:ts]))))))

;;
;; Add new samples (duplicates assert an error)
;;

(defn- as-sample [user inst [date value]]
  {:user user :inst inst :ts (dt/as-utc date) :value value})

(defn add-samples 
  "data is a sequence of date-value pairs"
  [user instrument data]
  (let [user (as-sql-user user)
	inst (as-sql-instrument instrument)]
    @(cql/conj! samples (map (partial as-sample user inst) data))))

;;
;; Support
;;

(defn get-last-sample-time [user inst]
  (dt/from-utc
   (first
    @(-> (inst-samples (as-sql-user user) (as-sql-instrument inst))
	 (cql/sort [:ts#desc])
	 (cql/limit 1)
	 (cql/pick [:ts])))))