(ns experiment.infra.models
  (:use noir.core)
  (:require
   [noir.response :as response]
   [noir.request :as request]
   [somnium.congomongo :as mongo]
   [clojure.walk :as walk]
   [clojure.string :as str]
   [clojure.tools.logging :as log])
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
  "A predicate to test that we have a valid model:
   :_id field ala mongo with valid ObjectID
   :type field indicating type of the model"
  [model]
  (and (:type model)
       (:_id model)
       (instance? ObjectId (:_id model))))

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
  "Performs a select-keys on client data so we don't store
   illegal client-side slots on the server or send server-side
   slots to the client.  Default is to be permissive."
  (fn [model] (when-let [type (:type model)]
                (keyword type))))

(defmulti server->client-hook
  "An optional function that is the identity fn by default which
   takes the server model and transforms it to a public/client
   view before serialization"
  (fn [model]
    (name (:type model))))

(defmulti client->server-hook
  "The import hook runs on the internal representation of the model
   after import but before the object is saved to the underlying store"
  (fn [model]
    (name (:type model))))

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
        (model-collection {:type type}))))

(defmethod db-reference-params :default
  [model]
  [])

(defmethod public-keys :default
  [model]
  (keys model))

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

(defn embedded-objectid? [id]
  (and (= (count id) 25)
       (= (first id) \*)))

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
  (dissoc (assoc smodel :id (serialize-id (:_id smodel))) :_id))

(defn deserialize-model-id
  "Import a foregin model-id as a local ID"
  [cmodel]
  (if (:id cmodel)
    (dissoc (assoc cmodel :_id (deserialize-id (:id cmodel))) :id)
    cmodel))

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
    (assoc model key (deserialize-dbref ns id))
    model))

(defn deserialize-model-refs [cmodel]
  "Import model references from the client"
  (reduce deserialize-model-ref cmodel (db-reference-params cmodel)))

;; ### Filter public-keys on import/export for safety

(defn filter-public-keys
  "Must define public-keys for safety purposes"
  [cmodel]
  (if-let [keys (public-keys (conj cmodel [:id :type]))]
    (select-keys cmodel keys)
    cmodel))
	
(defn as-dbref
  "Return a Mongo DBRef for a model object"
  ([model]
     (let [{:keys [type _id]} model]
       (assert (and type _id))
       (mongo/db-ref type _id)))
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

(defn safe-resolve-dbref [& args]
  (try
    (apply resolve-dbref args)
    (catch java.lang.Throwable e nil)))

(defn resolve-dbref
  ([ref]
     (assert (mongo/db-ref? ref))
     (somnium.congomongo.coerce/coerce (.fetch ^DBRef ref) [:mongo :clojure]))
  ([coll id]
     (assert (or (keyword? coll) (string? coll)))
     (mongo/fetch-one coll :where {:_id (as-oid id)})))

(defn assign-uid [model]
  (if (not (:_id model))
    (assoc model :_id (str "*" (ObjectId/get)))))
  

;; Extend DBRef with IDeref protocol for dereferencing?
;;
;; (extend DBRef
;;   clojure.lang.IDeref
;;   {:deref resolve-dbref})

;; ### Support for sub-objects and partial object updates

(defn lookup-location
  "Resolve string or keyword location and indirect into
   a model object so we can extract sub-content from the results
   of a mongo query using said location"
  [model location]
  (cond (string? location)
        (let [fields (str/split location #"\.")]
          (get-in model (map keyword fields)))
        (keyword? location)
        (model location)))

(defn submodel-path
  "Make a mongo-friendly sub-object path from a dotted location
   string, an array of keys"
  [location leaf]
  (if (sequential? location)
    (str/join \. (conj (map name location) (name leaf)))
    (str/join \. (list (name location) (name leaf)))))

(defn submodel-slot-paths
  "Return a map from model keys to keys that are paths
   to the keys of an embedded model"
  [model path]
  (zipmap (keys model)
          (map (comp (partial submodel-path path) name)
               (keys model))))

(defn- update-by-modifiers
  ([model]
     (let [bare (dissoc model :_id :id :type)]
       {:$set bare}))
  ([submodel path]
     {:$set (clojure.set/rename-keys
             (dissoc submodel :_id :id :type)
             (submodel-slot-paths submodel path))}))

(defmacro nil-on-empty [body]
  `(let [result# ~body]
     (when (not (empty? result#))
       result#)))


(defn translate-options
  "Convert our options to an mongo argument list"
  [options]
  (cond (empty? options) '()
        (keyword? (first options)) options
        true (cons :where options)))

;; ### Main internal API for Client-Server transforms

(defn server->client
  "Convert a server-side object into a map that is ready
   for JSON encoding and use by a client of the system"
  [smodel]
  (cond (empty? smodel)
	nil
	(map? smodel)
	(do ;; (log/spy smodel)
	    (-> smodel
		server->client-hook
		filter-public-keys
		serialize-model-id
		serialize-model-refs))
	(sequential? smodel)
	(vec (doall (map server->client smodel)))
	true
	(response/status
         500
         (format "Cannot export model %s" smodel))))

(defn client->server
  "Take a map transmitted from a client and convert it
   into a server-side object suitable to be stored or
   manipulated"
  [cmodel]
  (-> cmodel
      filter-public-keys
      deserialize-model-id 
      deserialize-model-refs
      client->server-hook))

(defn new-client->server
  "When a client creates a new object on the server, we
   don't transform the :id field"
  [cmodel]
  (-> cmodel
      filter-public-keys
      deserialize-model-refs
      client->server-hook))


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
  [model]
  (assert (not (model? model)))
  (if (valid-model? model)
    (mongo/insert! (model-collection model) (create-model-hook model))
    (noir.response/status 500 "Invalid Model")))

(defn update-model!
  [model]
  (if (valid-model? model)
    (mongo/update! (model-collection model)
                   (select-keys model [:_id])
                   (update-by-modifiers (update-model-hook model))
                   :upsert false)
    (noir.response/status 500 "Invalid Model")))

(defn modify-model!
  [model modifier]
  (assert (map? modifier))
  (mongo/update! (model-collection model)
                 (select-keys model [:_id])
                 modifier
                 :upsert false))
                 
(defn fetch-model
  [type & options]
  (nil-on-empty
   (apply mongo/fetch-one
          (model-collection {:type type})
          (translate-options options))))

(defn fetch-models
  [type & options]
  (apply mongo/fetch
	 (model-collection {:type type})
	 (translate-options options)))

(defn delete-model!
  [model]
  (assert (:type model) (:_id model))
  (delete-model-hook model)
  (mongo/destroy! (model-collection {:type (:type model)})
		  (select-keys model [:_id]))
  true)


;; ------------------------------------------
;; Client-Server SubModels API
;; ------------------------------------------
;;
;; The Submodel API provides the same abstraction over a document
;; database as the primary CRUD API, but allows the addition of
;; a location specifier which operates on embedded objects.
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
  [model location submodel]
  (let [new (assign-uid submodel)]
    (mongo/update! (model-collection model)
                   (select-keys model [:_id])
                   (update-by-modifiers
                    new
                    (submodel-path location new)))))

(defn get-submodel [model location]
  (lookup-location
   (mongo/fetch-one (model-collection model)
                    :where (select-keys model [:_id])
                    :only [location])
   location))

(defn get-submodels
  [model location]
  (get-submodel model location))

(defn set-submodel!
  [model location submodel]
  (mongo/update! (model-collection model)
                 (select-keys model [:_id])
                 (update-by-modifiers (client->server submodel)
                                      location)
                 :upsert false))

(defn delete-submodel!
  [model location]
  (mongo/update! (model-collection model)
                 (select-keys model [:_id])
                 {:$unset {location 1}}))
  
;; Legacy
(defmethod annotate-model! :default
  [model location annotation]
  (mongo/update! (model-collection model)
                  (select-keys model [:_id])
                  {:$push { location annotation }}
                  :upsert false))

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
		    criteria {:$set {field value}} :multiple true)))


