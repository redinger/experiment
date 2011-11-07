(ns experiment.infra.models
  (:use noir.core)
  (:require
   [noir.response :as response]
   [noir.request :as request]
   [somnium.congomongo :as mongo]
   [clojure.walk :as walk]
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
  :type)

(defmulti db-reference-params
  "Which parameters coming from a client are the IDs of
   foreign objects that should be stored as DB refs?"
  :type)

(defmulti model-collection 
  "Maps a model to a mongodb collection.  Embedded models
   are TBD, but I imagine we'll return a vector that includes
   the parent's collection + id + path"
  :type)

(defmulti client-keys 
  "Performs a select-keys on client data so we don't store
   illegal client-side slots on the server or send server-side
   slots to the client.  Default is to be permissive."
  :type)

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
	

;; =================================
;; Model CRUD API
;; =================================

(defmulti create-model! :type)
(defmulti update-model! :type)
(defmulti fetch-model (fn [type & options] type))
(defmulti fetch-models (fn [type & options] type))
(defmulti delete-model! :type)

(defn translate-options
  "Convert our options to an mongo argument list"
  [options]
  options)

(defn export-model [smodel]
  (cond (empty? smodel)
	nil
	(map? smodel)
	(do (log/spy smodel)
	    (-> smodel
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
      import-model-refs))

(defn import-new-model [cmodel]
  (-> cmodel
      filter-client-keys
      import-model-refs))

;;
;; Default Model behaviors
;;

(defmethod valid-model-params? :default
  [model]
  true)

(defmethod model-collection :default
  [model]
  (assert (:type model))
  (name (:type model)))

(defmethod db-reference-params :default
  [model]
  [])

(defmethod client-keys :default
  [model]
  (keys model))

;;
;; Default API implementation
;;

(defmethod create-model! :default
  [model]
  (assert (not (model? model)))
  (if (valid-model-params? model)
    (mongo/insert! (model-collection model) model)
    "Error"))

(defn- update-by-modifiers
  [model]
  (let [bare (dissoc model :_id :id)]
    {:$set bare}))

(defmethod update-model! :default
  [model]
  (if (valid-model-params? model)
    (.getError
     (mongo/update! (model-collection model)
		    {:_id (:_id model)}
		    (update-by-modifiers model)
		    :upsert false))
    "Error"))
  
(defmethod fetch-model :default [type & options]
  (apply mongo/fetch-one
	 (model-collection {:type type})
	 (translate-options options)))

(defmethod fetch-models :default [type & options]
  (apply mongo/fetch
	 (model-collection {:type type})
	 (translate-options options)))

(defmethod delete-model! :default [model]
  (assert (:type model) (:_id model))
  (mongo/destroy! (model-collection {:type (:type model)})
		  {:_id (:_id model)})
  true)


;;
;; Client Model translation API
;;

;; (defmulti serialize-client-object
;; ;;  "Convert a server-side object to JSON for transmission to a backbone client"
;;   (fn [obj]
;;     (if (sequential? obj)
;;       (.toString (:type (first obj)))
;;       (.toString (:type obj)))))

;; ;;
;; ;; Generic object translation methods
;; ;;

;; (defmethod serialize-client-object :default
;;   [object]
;;   (cond
;;    (nil? object) nil
;;    (sequential? object)
;;    (map serialize-client-object object)
;;    (associative? object)
;;    (add-db-refs
;;     (dissoc (assoc object :id (str (:_id object))) :_id))
;;    true object))


;; (defmethod serialize-client-object "experiment"
;;   [object]
;;   (clojure.walk/postwalk (fn [obj]
;; 			   (cond (= obj :_id) :id
;; 				 (mongo/db-ref? obj) (convert-dbref obj)
;; 				 (instance? ObjectId obj) (.toString obj)
;; 				 true obj))
;; 			 object))

;; ;; 

;; (defn deserialize-client-object
;;   "Take any properly formatted json object in parsed form and
;;    extract an object with an :id field and a :type field"
;;   [json]
;;   (assert (:type json))
;;   (if (and (:id json) (= (count (:id json)) 24))
;;     (dissoc (assoc json :_id (mongo/object-id (:id json))) :id)
;;     json))

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
