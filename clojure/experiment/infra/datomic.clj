
(ns experiment.infra.datomic
  (:require [experiment.infra.models :as models])
  (:use [datomic.api :only (db q) :as d]))

(defn attr [attr-name value-type cardinality]
  {:db/id          (d/tempid :db.part/db)
   :db/ident       attr-name
   :db/valueType   (keyword "db.type" (name value-type))
   :db/cardinality (keyword "db.cardinality" (name cardinality))
   :db.install/_attribute :db.part/db})

;; type field must be added under model abstraction
(def schema
  [(attr :user/id       :uuid   :one)
   (attr :user/name     :string :one)
   (attr :user/username :string :one)
   (attr :user/uname    :string :one)
   (attr :user/password :string :one)
   (attr :user/email    :string :one)
   ])

(def uri "datomic:mem://experiment")

(defonce conn (atom nil))

(defn start []
  (d/create-database uri)
  (reset! conn (d/connect uri))
  @(d/transact @conn schema))

(defn stop []
  (d/delete-database uri)
  (reset! conn nil))

(defn model->datomic [model]
  (let [model-type (:type model)]
    (reduce (fn [m [k v]]
              (assoc m (keyword (name model-type) (name k)) v))
            {} (dissoc model :type))))

(defn datomic->model [e]
  (let [model-type (keyword (namespace (first (keys e))))]
    (reduce (fn [m k]
              (assoc m (keyword (name k)) (k e)))
            {} (remove #{:db/id} (keys e)))))

(defn create-model! [model]
  (assert (not (models/model? model)))
  (when (models/valid-model? model)
    (let [id (java.util.UUID/randomUUID)]
      @(d/transact @conn
                   [(assoc (model->datomic (assoc model :id id))
                      :db/id (d/tempid :db.part/user))]))))

(defn fetch-models [& options]
  (let [[model-type query-map] options
        query (reduce (fn [q [k v]]
                        (let [a (keyword (name model-type) (name k))]
                          (conj q ['?e a v])))
                      '[:find ?e :where] query-map)
        dbval (db @conn)]
    (map #(datomic->model (d/entity dbval (first %)))
         (q query dbval))))

(comment
  ;; fetch-model:
  :user {:username "eslick"}
  ;; datomic query:
  [:find ?e :where [?e :user/username "eslick"]]

  ;; fetch-model:
  :event {:$gt {:start <date>}}
  ;; datomic query (?):
  [:find ?e :where
   [?e :event/start ?d]
   [(> (.getTime ?d) (.getTime <date>))]])

;; (:services :preferences :name :happy :username :uname : :preferenceshappy :journals :email :trackers :type :_id :trials :permissions :preferences:study1-consented :updates :password)

