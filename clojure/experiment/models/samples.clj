(ns experiment.models.samples
  (:use experiment.infra.models)
  (:require [clojureql.core :as cql]
	    [clj-time.core :as time]
            [clojure.java.jdbc :as sql]
	    [experiment.libs.datetime :as dt]))

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
     [:value :double]
     ["FOREIGN KEY(user)" "references" "users(id)"]
     ["FOREIGN KEY(inst)" "references" "instruments(id)"])
    (sql/create-table
     :records
     [:user :int "NOT NULL"]
     [:inst :int "NOT NULL"]
     [:ts :bigint "NOT NULL"]
     [:value "varchar(256)"]
     ["FOREIGN KEY(user)" "references" "users(id)"]
     ["FOREIGN KEY(inst)" "references" "instruments(id)"])
    (sql/do-commands
     "CREATE UNIQUE INDEX samples_idx ON samples (user,inst,ts)"
     "CREATE UNIQUE INDEX records_idx ON records (user,inst,ts)")))

(defn drop-tables []
  (comment
  (sql/with-connection db
    (doall (map sql/drop-table [:users :instruments :samples :records])))))

;;
;; Clojure QL based API
;;

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

(def samples (cql/table db :samples))
(def records (cql/table db :records))

(defn numeric-data? [instrument]
  (:numeric? instrument))

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

(defn instrument-table [user instrument]
  (-> (if (numeric-data? instrument)
        samples
        records)
      (cql/select (cql/where (= :user (as-sql-user user))))
      (cql/select (cql/where (= :inst (as-sql-instrument instrument))))))

;;
;; Get sample series
;;

(defn valid-instrument? [instrument]
  (and (model? instrument)
       (= (:type instrument) "instrument")))

(defn- as-sample [user instrument [dt val]]
  (assert (and dt val))
  {:user (as-sql-user user)
   :inst (as-sql-instrument instrument)
   :ts (dt/as-utc dt)
   :value val})

(defn add-samples
  "data is a sequence of date-value pairs"
  [user instrument data]
  (assert (valid-instrument? instrument))
  (clojure.tools.logging/spy data)
  @(cql/conj! (instrument-table user instrument)
              (vec (map (partial as-sample user instrument) data))))

(defn reset-samples [user instrument]
  @(cql/disj! (instrument-table user instrument)
              (cql/where (= :user (as-sql-user user)))))

(defn remove-samples [user instrument & [start end]]
  ;; TBD
  )

(defn- sample-pairs [convert?]
  (fn [results]
    (vec (map (fn [{:keys [ts value]}] [(if convert? (dt/from-utc ts) ts) value])
              results))))

(defn get-samples
  "Get a vector of time / value items.  An instrument can post
   process returned data to format - the post processor takes
   "
  ([user instrument]
     (get-samples user instrument 0 (dt/now) true))
  ([user instrument start]
     (get-samples user instrument start (dt/now) true))
  ([user instrument start end]
     (get-samples user instrument start end true))
  ([user instrument start end convert?]
     (assert (valid-instrument? instrument))
     (let [start (dt/as-utc start)
	   end (dt/as-utc end)]
       ((sample-pairs convert?)
        @(-> (instrument-table user instrument)
             (cql/select (cql/where (and (>= :ts start) (<= :ts end))))
             (cql/project [:ts :value])
             (cql/sort [:ts]))))))

(defn get-sample-range
  "Return a pair of datestamps if data exists or nil if not, the
   pair indicate the first and last timestamps for that user/inst pair"
  [user instrument]
  (let [[minmax] @(-> (instrument-table user instrument)
                      (cql/aggregate [[:max/ts :as :max] [:min/ts :as :min]]))]
    (update-in (update-in minmax [:max] dt/from-utc)
               [:min] dt/from-utc)))

(defn get-last-sample-time
  "Return the last timestamp on which we recorded data for this user
   and instrument - a number or nil if nothing"
  [user instrument]
  (:max (get-sample-range user instrument)))
     
  

