(ns experiment.infra.datomic
  (:require clojure.set
            [clojure.string :as str]
            [clojure.walk :as walk]
            [clojure.tools.logging :as log]
            [experiment.libs.datetime :as dt]
            [noir.response :as response]
            [noir.request :as request]
            [clodown.core :as md]
            [experiment.infra.session :as session])
  (:use [datomic.api :only (db q) :as d]))

;;; Datomic 

(defn attr [attr-name value-type & options]
  (let [cardinality (if (some #{:many} options)
                      :db.cardinality/many
                      :db.cardinality/one)]
    {:db/id          (d/tempid :db.part/db)
     :db/ident       attr-name
     :db/valueType   (keyword "db.type" (name value-type))
     :db/cardinality cardinality
     :db.install/_attribute :db.part/db}))

;; type field must be added under model abstraction
(def schema
  [[:user/id          :uuid]
   [:user/name        :string]
   [:user/username    :string]
   [:user/uname       :string]
   [:user/password    :string]
   [:user/email       :string]
   [:user/service     :ref    :many]

   ;; service/id is unique to user, not db - need to change client for this
   [:service/id       :string] 
   [:service/user     :string] ;; creds
   [:service/email    :string] ;; creds
   [:service/password :string] ;; creds
   [:service/token    :string] ;; oauth
   [:service/secret   :string] ;; oauth
   [:service/userid   :string] ;; for zeo
   [:service/segment1 :string] ;; for strava
   [:service/segment2 :string] ;; for strava
   
   ])

(def uri "datomic:mem://experiment")

(defonce conn (atom nil))

(defn start []
  (d/create-database uri)
  (reset! conn (d/connect uri))
  @(d/transact @conn (mapv #(apply attr %) schema)))

(defn stop []
  (d/delete-database uri)
  (reset! conn nil))

;;; Predicates

(defn model? [model]
  (and (map? model)
       (:id model)
       (:type model)))

(defn submodel? [model]
  (and (model? model)
       (:submodel model)))

;;; Multimethods

(defn model-type [model]
  (when-let [type (:type model)]
    (keyword type)))

(defmulti valid-model? model-type)
(defmulti db-reference-params model-type)
(defmulti model-collection model-type)
(defmulti public-keys model-type)
(defmulti import-keys model-type)
(defmulti index-keys model-type)
(defmulti server->client-hook model-type)
(defmulti client->server-hook model-type)
(defmulti make-annotation model-type)

;;; Multimethod defaults

(defmethod valid-model? :default
  [model]
  true)

(defmethod model-collection :default
  [model]
  (if (map? model)
    (do (assert (:type model))
        (name (:type model)))
    (do (assert (or (string? model) (keyword? model)))
        (name model))))

(defmethod db-reference-params :default
  [model]
  [])

(defmethod public-keys :default
  [model]
  nil)

(defmethod import-keys :default
  [model]
  nil)

(defmethod index-keys :default
  [model]
  nil)

(defmethod server->client-hook :default
  [model]
  model)

(defmethod client->server-hook :default
  [model]
  model)

;;; Markdown

;; ### Utilities for augmenting fields

(defn markdown-convert* [model k]
  (let [new-key (keyword (str (name k) "-html"))
        orig (model k)]
    (if (> (count orig) 1)
      (assoc model new-key (md/mdp orig))
      (assoc model new-key "<p><p/>"))))

(defn markdown-convert [model k & ks]
  (reduce markdown-convert* model (cons k ks)))

;;; Filtering for clients

;; ### Filter public-keys on import/export for safety

(defn filter-public-keys
  "Must define public-keys for safety purposes"
  [cmodel]
  (if-let [keys (public-keys cmodel)]
    (select-keys cmodel (conj keys :_id :id :type))
    (throw (java.lang.Error. (str "public-keys not defined for " (:type cmodel))))))

(defn filter-import-keys
  "Must define import-keys for safety purposes"
  [cmodel]
  (when (cmodel nil)
    (log/warnf "Importing 'nil' as key in model:%n%s" cmodel))
  (if-let [keys (import-keys cmodel)]
    (select-keys cmodel (conj keys :_id :id :type :submodel))
    (throw (java.lang.Error. (str "import-keys not defined for " (:type cmodel))))))

;;; nulls

(defn null-value-keys [model]
  (map first (filter #(nil? (second %)) model)))

(def nil-on-empty seq)

;;; client-server communication

;; ### Handle canonical server form to client transforms

(defn server->client*
  ([node]
     (server->client* node false))
  ([node force]
     (if (or (model? node) (submodel? node) force)
       (-> node
           server->client-hook
           filter-public-keys)
       node)))

#_(defn server->client
  "Convert a server-side object into a map that is ready
   for JSON encoding and use by a client of the system"
  ([smodel]
     (server->client smodel false))
  ([smodel force]
     (cond (empty? smodel)
           nil
           (map? smodel)
           (-> (if force
                 (server->client* smodel true)
                 (walk/postwalk server->client* smodel))
               serialize-model-id
               serialize-model-refs)
           (sequential? smodel)
           (vec (doall (map server->client smodel)))
           true
           (response/status
            500
            (format "Cannot export model %s" smodel)))))

#_(defn client->server
  "Take a map transmitted from a client and convert it
   into a server-side object suitable to be stored or
   manipulated"
  [cmodel]
  (-> cmodel
      filter-import-keys
      deserialize-model-id 
      deserialize-model-refs
      client->server-hook))

#_(defn new-client->server
  "When a client creates a new object on the server, we
   don't transform the :id field"
  [cmodel]
  (-> cmodel
      filter-import-keys
      deserialize-model-refs
      client->server-hook))

;;; Client-server model API

;; Legacy
(defmulti annotate-model! (fn [type field anndo] (keyword type)))

;; Model Evolution Hooks

(defmulti create-model-hook (comp keyword :type))
(defmulti update-model-hook (comp keyword :type))
(defmulti delete-model-hook (comp keyword :type))

(defmethod create-model-hook :default
  [model]
  (update-model-hook model))

(defmethod update-model-hook :default
  [model]
  model)

(defmethod delete-model-hook :default
  [model]
  model)

;;; Model CRUD API implementation

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

(defn fetch-model
  "Get a model from the database"
  [& options]
  (first (apply fetch-models options)))

(defn delete-model!
  "Delete a model from the database; must have a valid :id"
  [model]
  (delete-model-hook model)
  (let [eid (q '[:find ?e :in $ ?id :where [?e :experiment/id ?id]]
               (db @conn) (:id model))]
    @(d/transact @conn [[:db.fn/retractEntity eid]])))

(defn modify-model!
  [])

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
