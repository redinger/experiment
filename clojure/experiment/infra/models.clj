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

;;
;; Model Behavior Protocol
;;

(defn model?
  "A predicate to test that we have a valid model:
   :_id field ala mongo with valid ObjectID
   :type field indicating type of the model"
  [model]
  (and (:type model)
       (:_id model)
       (instance? ObjectId (:_id model))))

(defmulti valid-model-params? 
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


(defmulti client-keys 
  "Performs a select-keys on client data so we don't store
   illegal client-side slots on the server or send server-side
   slots to the client.  Default is to be permissive."
  (fn [model] (when-let [type (:type model)]
                (keyword type))))

(defmulti export-hook
  "An optional function that is the identity fn by default which
   takes the internal model and transforms it to another version of
   an internal model prior to export"
  (fn [model]
    (name (:type model))))

(defmulti import-hook
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
;; MongoDB Helpers
;;

(defn objectid? 
  [id]
  (= (type id) ObjectId))

(defn serialize-id
  "Convert a MongoDB ID for client use"
  [id]
  (assert (instance? ObjectId id))
  (str id))

(defn deserialize-id
  "Convert a foreign ID reference to an Mongo ObjectId"
  [id]
  (mongo/object-id id))

(defn- serialize-dbref
  "Convert a DBRef object to a pair referencing a namespace and UID"
  [dbref]
  (assert (instance? DBRef dbref))
  (let [ref (.getRef dbref)
	id (.getId dbref)]
    (when (and ref id)
      [ref (.toString id)])))

(defn- deserialize-dbref
  [ref]
  (assert (and (= (count ref) 2) (every? string? ref)))
  (mongo/db-ref (first ref) (mongo/object-id (second ref))))

;;
;; Model import/export handlers for MongoDB
;;

;; Main object IDs

(defn export-model-id
  "Export a local model-id as a foreign ID"
  [smodel]
  (dissoc (assoc smodel :id (serialize-id (:_id smodel))) :_id))

(defn import-model-id
  "Import a foregin model-id as a local ID"
  [cmodel]
  (dissoc (assoc cmodel :_id (deserialize-id (:id cmodel))) :id))

;; Embedded DBRefs

(defn- export-ref
  "If the object is a DBRef, convert to client format"
  [ref]
  (if (mongo/db-ref? ref)
    (serialize-dbref ref)
    ref))

(defn export-model-refs [smodel]
  (walk/postwalk export-ref smodel))

(defn- import-model-ref [model key]
  (if-let [[ns id] (model key)]
    (assoc model key (deserialize-dbref ns id))
    model))

(defn import-model-refs [cmodel]
  "Import model references from the client"
  (reduce import-model-ref cmodel (db-reference-params cmodel)))

;; Filter client-keys on import/export for safety

(defn filter-client-keys
  "Must define client-keys for safety purposes"
  [cmodel]
  (let [keys (client-keys cmodel)]
;;    (assert keys) ;; Comment out for development
    (if keys (select-keys cmodel keys)
	cmodel)))
	
(defn as-dbref
  ([model]
     (let [{:keys [type _id]} model]
       (assert (and type _id))
       (mongo/db-ref type _id)))
  ([name id]
     (mongo/db-ref name id)))

(defn as-oid [id]
  (cond (objectid? id) id
	(string? id) (mongo/object-id id)
	true (assert (str "Unrecognized id: " id))))

(defn oid? [id]
  (objectid? id))

(defn resolve-dbref [ref & [id]]
  (if id
    (do (assert (or (keyword? ref) (string? ref)))
	(mongo/fetch-one ref :where {:_id (as-oid id)}))
    (do (assert (mongo/db-ref? ref))
	(mongo/fetch-one (.getRef ref) :where {:_id (.getId ref)}))))

;;(extend DBRef
;;  clojure.lang.IDeref
;;  {:deref resolve-dbref})

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

(defmacro nil-on-empty [body]
  `(let [result# ~body]
     (when (not (empty? result#))
       result#)))

;; =================================
;; Model CRUD API
;; =================================

(defmulti create-model! :type)
(defmulti update-model! :type)
(defmulti modify-model! :type)
(defmulti update-model-pre (comp str :type))
(defmulti fetch-model (fn [type & options] type))
(defmulti fetch-models (fn [type & options] type))
(defmulti delete-model-pre (comp str :type))
(defmulti delete-model! :type)
(defmulti annotate-model! (fn [type field anno] type))

(defn translate-options
  "Convert our options to an mongo argument list"
  [options]
  options)

(defn export-model [smodel]
  (cond (empty? smodel)
	nil
	(map? smodel)
	(do ;; (log/spy smodel)
	    (-> smodel
		export-hook
		filter-client-keys
		export-model-id
		export-model-refs))
	(sequential? smodel)
	(vec (doall (map export-model smodel)))
	true
	(throw (java.lang.Error. (format "Cannot export model %s" smodel)))))

(defn import-model [cmodel]
  (-> cmodel
      filter-client-keys
      import-model-id 
      import-model-refs
      import-hook))

(defn import-new-model [cmodel]
  (-> cmodel
      filter-client-keys
      import-model-refs
      import-hook))

;;
;; Default Model behaviors
;;

(defmethod valid-model-params? :default
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

(defmethod client-keys :default
  [model]
  (keys model))

(defmethod export-hook :default
  [model]
  model)

(defmethod import-hook :default
  [model]
  model)

;;
;; Default Model CRUD API implementation
;;

(defmethod create-model! :default
  [model]
  (assert (not (model? model)))
  (if (valid-model-params? model)
    (mongo/insert! (model-collection model) (update-model-pre model))
    "Error"))

(defn- update-by-modifiers
  [model]
  (let [bare (dissoc model :_id :id :type)]
    {:$set bare}))

(defmethod update-model-pre :default
  [model]
  model)

(defmethod update-model! :default
  [model]
  (if (valid-model-params? model)
    (.getError
     (mongo/update! (model-collection model)
                    {:_id (:_id model)}
                    (update-by-modifiers (update-model-pre model))
                    :upsert false))
    "Error"))

(defmethod modify-model! :default
  [model modifier]
  (assert (map? modifier))
  (mongo/update! (model-collection model)
                 {:_id (:_id model)}
                 modifier
                 :upsert false))
                 

(defmethod annotate-model! :default
  [model location annotation]
  (.getError
   (mongo/update! (model-collection model)
                  {:_id (:_id model)}
                  {:$push { location annotation }}
                  :upsert false)))

(defn set-submodel! [model location data]
  (.getError
   (mongo/update! (model-collection model)
                  {:_id (:_id model)}
                  {:$set { location data }})))

(defn get-submodel [model location]
  (lookup-location
   (mongo/fetch (model-collection model)
                :where {:_id (:_id model)}
                :only [location])))

(defmethod fetch-model :default [type & options]
  (nil-on-empty
   (apply mongo/fetch-one
          (model-collection {:type type})
          (translate-options options))))

(defmethod fetch-models :default [type & options]
  (apply mongo/fetch
	 (model-collection {:type type})
	 (translate-options options)))

(defmethod delete-model! :default [model]
  (assert (:type model) (:_id model))
  (mongo/destroy! (model-collection {:type (:type model)})
		  {:_id (:_id model)})
  true)

;; Utilities

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


;;
;; Compact model definition macro
;;

;; (defmacro defmodel [name & {:as options}]
;;   ...)

;; Name the model
;; - mongo collection
;; - validity testing
;; - client key set
;; - client HB template(s)
