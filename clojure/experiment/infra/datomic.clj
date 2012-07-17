
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


;; ISE: Comments and Tags are common across objects, common namespace or is duplication not a sin in this case?

(comment
  ;; Instrument
  [(attr :instrument/id       :uuid   :one)
   (attr :instrument/variable :string :one)
   (attr :instrument/service  :string :one)
   (attr :instrument/src      :string :one) ;; dispatch value for methods
   (attr :instrument/description :string :one)
   (attr :instrument/nicknames :?     :one) ;; array of strings
   (attr :instrument/comments  :?     :?)   ;; subcollection of Comment objs
   ]
  ;; Tracker
  [(attr :tracker/id :uuid :one)
   (attr :tracker/instrument :ref :one) ;; by reference
   (attr :tracker/user :ref :one) ;; by reference
   (attr :tracker/schedule :ref :one) ;; embedded
  ]
  ;; Treatment
  [(attr :treatment/id   :uuid :one)
   (attr :treatment/name :string :one)
   (attr :treatment/description :string :one)
   (attr :treatment/tags :? :?) ;; array of strings
   (attr :treatment/reminder :string :one)
   (attr :treatment/dynamics :? :?) ;; structure of simple keys + integer values
   (attr :treatment/user :? :?) ;; entity reference
   (attr :treatment/comments :? :?) ;; subcollection of Comment objs
   (attr :treatment/editors :? :?) ;; list of entities
   ]
  ;; Experiment
  [(attr :experiment/id        :uuid :one)
   (attr :experiment/title     :string :one)
   (attr :experiment/treatment :string :one)
   (attr :experiment/outcome   :? :one) ;; Primary outcome, entity ref
   (attr :experiment/covariates :? :?) ;; Set of entity references 1-to-many
   (attr :experiment/schedule :? :?)   ;; embedded object or reference
   (attr :experiment/editors :? :?)    ;; as above
   (attr :experiment/comments :? :?)   ;; as above
   ]
  ;; Trial
  [(attr :trial/id :uuid :one)
   (attr :trial/user :? :one) ;; reference
   (attr :trial/experiment :? :one) ;; reference
   (attr :trial/start :date :one)
   (attr :trial/status :string :one)
   (attr :trial/channels :? :?) ;; array of strings
   ]
  ;; Schedule (see models/schedule.clj)
  [(attr :schedule/id :uuid :one)
   (attr :schedule/stype :string :one) ;; e.g. :daily, :weekly, :periodic
   (attr :times :? :?) ;; array of simple structures (blobs?)
   (attr :channels :? :?) ;; array of strings
   (attr :jitter :integer :one)
   (attr :event :? :one) ;; reference to a partial, anonymous event object (blob?)
   ]
  ;; Event - Ian will port this subsystem over on Thu/Fri
  []
  ;; Journal
  [(:id :date :enddate :content :annotation :sharing :short)]
  ;; Service (instance - stores user's tokens/credentials for a given service)
  [(attr :service/id :string :one) ;; unique to user, not db - need to change client for this
   (attr :service/user :? :one) ;; reference
   (attr :service/user :string :one) ;; creds
   (attr :service/email :string :one) ;; creds
   (attr :service/password :string :one) ;; creds
   (attr :service/token :string :one) ;; oauth
   (attr :service/secret :string :one) ;; oauth
   (attr :service/userid :string :one) ;; for zeo
   (attr :service/segment1 :string :one) ;; for strava
   (attr :service/segment2 :string :one) ;; for strava
   ]
   )
