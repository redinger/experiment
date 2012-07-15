(ns experiment.infra.models
  (:use
   noir.core)
  (:require
   clojure.set
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojure.tools.logging :as log]
   [experiment.libs.datetime :as dt]
   [somnium.congomongo :as mongo]
   [noir.response :as response]
   [noir.request :as request]
   [clodown.core :as md]
   [experiment.infra.session :as session])
  (:import [org.bson.types ObjectId]
           [com.mongodb DBRef]))

;; ------------------------------------------
;; Server-Client Model Framework
;; ------------------------------------------

;; This framework provides hooks for different data
;; models, implemented as standard maps, with a
;; convention of :type as the type specifier and
;; client-side ID of :id and server-side id of :_id
;; The APIs included here are somewhat MongoDB specific
;; still, but provide a good baseline abstraction for
;; models that live in a document DB and have different
;; behavior and valid slots in the server or client context.

(defn model?
  "A predicate to test that we have a valid parent model:
   :_id field ala mongo with valid ObjectID
   :type field indicating type of the model"
  [model]
  (and (:type model)
       (:_id model)
       (instance? ObjectId (:_id model))))

(declare embedded-objectid?)

(defn submodel? 
  "A predict to test that we have a valid submodel"
  [submodel]
  (and (:type submodel)
       (or (:submodel submodel)
           (and (:id submodel)
                (re-find #"SM" (:id submodel))))))

(defmulti valid-model? 
  "Enforces invariant properties of a specific model.  Model
   creation and updating both must satisfy this test for
   mutation to proceed.  Test that appropriate values
   exist and have coherent values."
  (fn [model] (when-let [type (:type model)]
                (keyword type))))

(defmulti db-reference-params
  "Which parameters coming from a client are the IDs of
   foreign objects that should be stored as DB refs?"
  (fn [model] (when-let [type (:type model)]
                (keyword type))))

(defmulti model-collection 
  "Maps a model to a mongodb collection.  Embedded models
   are TBD, but I imagine we'll return a vector that includes
   the parent's collection + id + path"
  (fn [model] (when-let [type (:type model)]
                (keyword type))))

(defmulti public-keys
  "Which server-side raw or derived (via hook) keys 
   to send to the client.  Default is to be permissive."
  (fn [model] (when-let [type (:type model)]
                (keyword type))))
  

(defmulti import-keys 
  "Performs a select-keys on client data so we don't store
   illegal client-side slots on the server."
  (fn [model] (when-let [type (:type model)]
                (keyword type))))

(defmulti index-keys 
  "Performs a select-keys on client data so we don't store
   illegal client-side slots on the server."
  (fn [model] (when-let [type (:type model)]
                (keyword type))))

(defmulti server->client-hook
  "An optional function that is the identity fn by default which
   takes the server model and transforms it to a public/client
   view before serialization"
  (fn [model]
    (keyword (:type model))))

(defmulti client->server-hook
  "The import hook runs on the internal representation of the model
   after import but before the object is saved to the underlying store"
  (fn [model]
    (keyword (:type model))))

(defmulti make-annotation
  "For embedded objects we expose a generic API for creating objects
   from client arguments dispatching on the :type field of the arguments"
  (fn [model]
    (keyword (:type model))))


;;
;; ## Default Model behaviors
;;

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

;;
;; ## MongoDB Helpers
;;

(defn objectid? [id]
  (= (type id) ObjectId))

(defn dbref? [ref]
  (= (type ref) DBRef))

(defn embedded-objectid? [id]
  (and (string? id)
       (= (first id) \S)
       (= (second id) \M)))

(defn serialize-id
  "Convert a MongoDB ID for client use"
  [id]
  (assert (or (objectid? id) (embedded-objectid? id)))
  (str id))

(defn deserialize-id
  "Convert a client ID reference to an ObjectId"
  [id]
  (assert (string? id))
  (if (embedded-objectid? id)
    id
    (mongo/object-id id)))

(defn- serialize-dbref
  "Convert a DBRef object to a pair referencing a namespace and UID"
  [dbref]
  (assert (instance? DBRef dbref))
  (let [ref (.getRef dbref)
	id (.getId dbref)]
    (when (and ref id)
      [ref (.toString id)])))

(defn- deserialize-dbref
  "Convert a client-side object reference pair to a DBRef"
  [ref]
  (assert (and (= (count ref) 2) (every? string? ref)))
  (mongo/db-ref (first ref) (mongo/object-id (second ref))))

;;
;; ## Model import/export handlers
;;

;; ### Convert object IDs to/from client

(defn serialize-model-id
  "Export a local model-id as a foreign ID"
  [smodel]
  (if (:_id smodel) ;; primary model
    (dissoc (assoc smodel :id (serialize-id (:_id smodel))) :_id)
    smodel ;; submodel is already serialized
    ))

(defn deserialize-model-id
  "Import a foregin model-id as a local ID"
  [cmodel]
  (if (:submodel cmodel)
    cmodel
    (dissoc (assoc cmodel :_id (deserialize-id (:id cmodel))) :id)))

;; ### Handle embedded DBRefs

(defn- serialize-ref
  "If the object is a DBRef, convert to client format"
  [ref]
  (if (mongo/db-ref? ref)
    (serialize-dbref ref)
    ref))

(defn serialize-model-refs [smodel]
  (walk/postwalk serialize-ref smodel))


(defn- deserialize-model-ref [model key]
  (if-let [[ns id] (model key)]
    (if (and ns id)
      (assoc model key (deserialize-dbref [ns id]))
      model)
    model))

(defn deserialize-model-refs [cmodel]
  "Import model references from the client"
  (reduce deserialize-model-ref cmodel (db-reference-params cmodel)))

;; ### Utilities for augmenting fields

(defn markdown-convert* [model k]
  (let [new-key (keyword (str (name k) "-html"))
        orig (model k)]
    (if (> (count orig) 1)
      (assoc model new-key (md/mdp orig))
      (assoc model new-key "<p><p/>"))))

(defn markdown-convert [model k & ks]
  (reduce markdown-convert* model (cons k ks)))

(defn ref-oid [ref]
  (when ref
    (cond (dbref? ref) (.getId ref)
          (model? ref) (:_id ref))))

(defn owner-as-bool
  "Converts a user reference field to a boolean
   if field reference(s) matches user or current-user"
  [model field & {:keys [user admins] :or {admins [] user (session/current-user)}}]
  (let [uid (:_id user)
        value (model field)
        users (concat (cond (nil? value)
                            (list)
                            (dbref? value)
                            (list value)
                            true
                            value)
                      admins)]
    (if (> (count (filter #(= uid (ref-oid %)) users)) 0)
      (assoc model field true)
      (assoc model field false))))

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

(defn as-dbref
  "Return a Mongo DBRef for a model object"
  ([model]
     (if (dbref? model) model
         (let [{:keys [type _id]} model]
           (assert (and type _id))
           (mongo/db-ref type _id))))
  ([name id]
     (mongo/db-ref name id)))

(defn as-oid
  "Ensure an ID string is an ObjectID"
  [id]
  (cond (objectid? id) id
	(string? id) (mongo/object-id id)
	true (assert (str "Unrecognized id: " id))))

(defn oid? [id]
  (objectid? id))

(defn resolve-dbref
  ([ref]
     (if (mongo/db-ref? ref)
       (somnium.congomongo.coerce/coerce (.fetch ^DBRef ref) [:mongo :clojure])
       ref))
  ([coll id]
     (assert (or (keyword? coll) (string? coll)))
     (mongo/fetch-one coll :where {:_id (as-oid id)})))

(defn safe-resolve-dbref [& args]
  (try
    (apply resolve-dbref args)
    (catch java.lang.Throwable e nil)))

(defn assign-uid [model]
  (if (not (:id model))
    (assoc model :id (str "SM" (ObjectId/get)))))

(defn embed-dbrefs [model]
  (clojure.walk/prewalk
   (fn [node]
     (if (dbref? node)
       (resolve-dbref node)
       node))
   model))


;; Extend DBRef with IDeref protocol for dereferencing?
;;
;; (extend DBRef
;;   clojure.lang.IDeref
;;   {:deref resolve-dbref})

;; ### Support for sub-objects and partial object updates

(defn location->keyword-array
  "Transform a location string to a sequence"
  [location]
  (cond (sequential? location) (map keyword location)
        (keyword? location) (vector keyword)
        true (map keyword (str/split location #"\."))))

(defn lookup-location
  "Resolve string or keyword location and indirect into
   a model object so we can extract sub-content from the results
   of a mongo query using said location"
  [model location]
  (get-in model (location->keyword-array location)))
        

(defn serialize-path
  "Make a mongo-friendly sub-object path from a dotted location
   string, an array of keys"
  ([location leaf]
     (if (sequential? location)
       (serialize-path (concat location (list leaf)))
       (serialize-path (list (name location) (name leaf)))))
  ([location]
     (if (sequential? location)
       (str/join \. (map name location))
       location)))

(defn- serialize-slot-paths
  "Return a map from model keys to keys that are paths
   to the keys of an embedded model"
  [model path]
  (zipmap (keys model)
          (map (comp (partial serialize-path path) name)
               (keys model))))

(defn null-value-keys [model]
  (map first (filter #(nil? (second %)) model)))

(defmacro nil-on-empty [body]
  `(let [result# ~body]
     (when (not (empty? result#))
       result#)))

(defn translate-options
  "Convert our options to an mongo argument list"
  [options]
  (let [lead (first options)]
    (assert lead)
    (cond (or (keyword? lead) (string? lead))
          (cond (empty? (rest options))
                (list lead)
                (map? (second options))
                (concat (list lead :where)
                        (rest options))
                true options)
          (map? lead)
          (concat (list (:type lead) :where (dissoc lead :type))
                  (rest options)))))

;; ### Handling conversions from canonical clojure form to/from mongo

(defn mongo->canonical* [obj]
  (cond (dt/date? obj)
        (dt/as-joda obj)
        true obj))

(defn mongo->canonical [obj]
  (walk/postwalk mongo->canonical* obj))

(defn canonical->mongo* [obj]
  (cond (dt/date? obj) (dt/as-java obj)
        true obj))

(defn canonical->mongo [obj]
  (walk/postwalk canonical->mongo* obj))
  
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

(defn server->client
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

(defn client->server
  "Take a map transmitted from a client and convert it
   into a server-side object suitable to be stored or
   manipulated"
  [cmodel]
  (-> cmodel
      filter-import-keys
      deserialize-model-id 
      deserialize-model-refs
      client->server-hook))

(defn new-client->server
  "When a client creates a new object on the server, we
   don't transform the :id field"
  [cmodel]
  (-> cmodel
      filter-import-keys
      deserialize-model-refs
      client->server-hook))

;; ### Merge over updates for API

(defn combine-names [a & names]
  (str/join
   "." 
   (if (sequential? (first names))
     (map name (cons a (first names)))
     (map name (cons a names)))))
  
(defn flatten-field-refs [base kvs]
  (zipmap (map (partial combine-names base) (keys kvs))
          (vals kvs)))

(defn flatten-fields [kvs]
  (reduce (fn [pairs [k v]]
            (if (and (map? v) (not (empty? v)))
              (merge pairs (flatten-field-refs k (flatten-fields v)))
              (assoc pairs k v)))
          {} kvs))

(defn update-by-modifiers
  ([model]
     (let [bare (dissoc model :_id :id :type)]
       {:$set (canonical->mongo (flatten-fields bare))}))
  ([submodel path]
     (let [flattened (canonical->mongo (flatten-fields submodel))]
       {:$set (clojure.set/rename-keys
               flattened
               (serialize-slot-paths flattened path))})))

;; ------------------------------------------
;; Client-Server Models API
;; ------------------------------------------

;; Basic API (see implementations below)
(declare create-model! fetch-model fetch-models
         update-model! modify-model! delete-model!)

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


;; Model CRUD API implementation

(defn create-model!
  "Create a new model in the database using all the model hooks defined
   above"
  [model]
  (assert (not (model? model)))
  (if (valid-model? model)
    (mongo/insert! (model-collection model)
                   (canonical->mongo
                    (create-model-hook model)))
    "Invalid Model"))

(defn update-model!
  "Update model merges the provided key-value pairs with the
   database key-value pair set.  Setting any key-value pair
   with embedded objects will overwrite the entire set (i.e.
   no merging or appending of elements in a value"
  [model]
  (if (and (:_id model) (:type model))
    (let [result (mongo/update! (model-collection model)
                                (select-keys model [:_id])
                                (update-by-modifiers
                                 (update-model-hook model))
                                :return-new? true)]
      (session/update-user! model)
      (if-let [err (.getError result)]
        "DB Error"
        true))
    "Invalid Model"))

(defn modify-model!
  "A cheap hack to open up the use of raw Mongo APIs for modifying
   a document."
  [model modifier]
  (assert (map? modifier))
  (mongo/update! (model-collection model)
                 (select-keys model [:_id])
                 (canonical->mongo modifier)
                 :upsert false))
                 
(defn fetch-model
  "Get a model from the database"
  [& options]
  (nil-on-empty
   (let [[type & args] (translate-options options)]
     (mongo->canonical
      (apply mongo/fetch-one (model-collection type) args)))))

(defn fetch-models
  "Get a seq of models from the database"
  [& options]
  (let [[type & args] (translate-options options)]
    (mongo->canonical
     (apply mongo/fetch (model-collection type) args))))

(defn delete-model!
  "Delete a model from the database; must have a valid :_id"
  [model]
  (delete-model-hook model)
  (mongo/destroy! (model-collection model) (select-keys model [:_id]))
  true)

;; Migration
;; -----------------------------------

;; Handle references
(def origin-ids* (atom (hash-map)))

(defn- reset-registry []
  (swap! origin-ids* (fn [_] (hash-map))))

(defn- register-object [old new newid]
  (println (:_id old) " -> " newid)
  (swap! origin-ids* (fn [reg] (assoc reg (:_id old) newid)))
  (if newid
    (assoc new :_id newid)
    new))
  
(defn- convert-dbref [node]
  (cond (objectid? node)
        (if-let [target (@origin-ids* node)]
          target
          (throw (java.lang.Error. (str "Target ID not found for " node))))
        (dbref? node)
        (let [target (@origin-ids* (.getId node))]
          (if target
            (as-dbref (.getRef node) target)
            (throw (java.lang.Error. (str "Target ID not found for " node )))))
        true
        node))

(defn- convert-dbrefs [object]
  (assoc (clojure.walk/prewalk convert-dbref (dissoc object :_id))
    :_id (:_id object)))

;; Migrate models

(defn migrate-models [origin destination coll query match &
                      {:keys [origin-wins inhibit debug]}]
  (let [origin-objs (mongo/with-mongo origin
                  (fetch-models coll (or query {})))]
    (mongo/with-mongo destination
      (when (not inhibit) (println "Writing " (count origin-objs)))
      (doseq [origin origin-objs]
        (when (= true debug) (println "Origin: " origin))
        (let [dest (fetch-model coll (select-keys origin match))
              new (if origin-wins
                    (merge dest origin)
                    (merge origin dest))
              new (convert-dbrefs
                   (if dest
                     (register-object origin new (:_id dest))
                     (dissoc new :_id)))]
          (when (= true debug) (println "Target: " dest))
          (when (= true debug) (println "New: " new))
          (when (sequential? debug) (println (select-keys new debug)))
          (when (not inhibit)
            (when (= debug true) (println "Writing"))
            (if (:_id new)
              (update-model! new)
              (let [created (create-model! new)]
                (register-object origin created (:_id created))
                created))))))))
        

;; ------------------------------------------
;; Client-Server SubModels API
;; ------------------------------------------
;;
;; The Submodel API provides the same abstraction over a document
;; database as the primary CRUD API, but allows the addition of
;; a location specifier which operates on embedded objects.
;;
;; Writes to mongo go through create-model-hook or update-model-hook
;;   then canonical->mongo
;; Reads from mongo go through mongo->canonical
;;
;; (set-submodel! parent "profile.addresses.id"
;;                {:type "address" :name "Joe User"})
;; =>
;; {:foo {:bar {:id {type "address" :name "Joe User"}}}}
;;

(declare create-submodel! set-submodel!
         get-submodel get-submodels
         delete-submodel!)

(defn create-submodel!
  [parent location submodel]
  (assert (and (model? parent) (:type submodel)))
  (let [new (create-model-hook (assign-uid submodel))
        pcoll (model-collection parent)
        pref (select-keys parent [:_id :type])
        path (serialize-path location (:id new))]
    ;; Ensure we have a fresh target to insert into
    (mongo/update! pcoll pref {:$set {path {}}
                               :$inc {"updates" 1}})
    ;; Insert all the slots
    (mongo/update! pcoll pref (update-by-modifiers new path))
    ;; Return the new model
    new))

(defn get-submodel
  ([parent location]
     (assert (and (:type parent) (:_id parent)))
     (let [parent (mongo/fetch-one (model-collection parent)
                                :where (select-keys parent [:_id])
                                :only [(serialize-path location)])]
       (mongo->canonical
        (lookup-location parent location)))))

;;(defn get-submodels
;;  [model type]
;;  (mongo/fetch (model-collection model)
;;               :where (select-keys model [:_id])
;;               :only (serialize-path (submodel-path model {:type type}))))

(defn set-submodel!
  [model location submodel]
  (let [updates (update-by-modifiers
                 (update-model-hook submodel)
                 location)]
    (mongo/update! (model-collection model)
                   (select-keys model [:_id])
                   updates
                   :upsert false)))

(defn delete-submodel!
  [model location]
  (let [sig (select-keys model [:type :_id])
        model (fetch-model (model-collection model) sig)
        location (location->keyword-array location)]
    (delete-model-hook
     (lookup-location model location))
    (mongo/update! (model-collection model) sig
                   (canonical->mongo
                    (update-in model (vec (butlast location))
                               (fn [coll]
                                 (dissoc coll (last location))))))))
     
;;    (delete-model-hook
;;     (get-submodel model location))
;;  (mongo/update! (model-collection model)
;;                 (select-keys model [:_id])
;;                 {:$unset {(log/spy (serialize-path location)) 1}}
;;                 :upsert false))

;; ## Misc Store Utilities

(defn unset-collection-field 
  ([type field]
     (unset-collection-field type field {}))
  ([type field criteria]
     (mongo/update! (model-collection {:type type})
		    criteria {:$unset {field 1}} :multiple true)))

(defn set-collection-field
  ([type field value]
     (set-collection-field type field value {}))
  ([type field value criteria]
     (mongo/update! (model-collection {:type type})
		    criteria {:$set {field value} :$inc {"updates" 1}} :multiple true)))


