(ns experiment.infra.models
  (:require
   [noir.response :as response]
   [noir.request :as request]
   [somnium.congomongo :as mongo])
  (:use noir.core))

;;
;; Model Management API
;;

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

(defn serialize-id [id]
  (str id))

(defn deserialize-id [id]
  (mongo/object-id id))

;;
;; Model CRUD API
;;

(defmulti create-model! :type)
(defmulti update-model! :type)
(defmulti fetch-model (fn [type & options] type))
(defmulti fetch-models (fn [type & options] type))
(defmulti delete-model! :type)

(defn translate-options
  "Convert our options to an mongo argument list"
  [options]
  options)

(defn objectid? 
  [id]
  (= (type id) org.bson.types.ObjectId))

(defn model?
  "A predicate to test that we have a valid model:
   :_id field ala mongo with valid ObjectID
   :type field indicating type of the model"
  [model]
  (and (:type model)
       (:_id model)
       (objectid? (:_id model))))

;;
;; Default implementations
;;

(defmethod valid-model-params? :default
  [model]
  true)

(defmethod db-reference-params :default
  [model]
  [])

(defmethod model-collection :default
  [model]
  (assert (:type model))
  (str (:type model) "s"))

(defmethod client-keys :default
  [model]
  model)

(defmethod create-model! :default
  [model]
  (assert (not (model? model)))
  (if (valid-model-params? model)
    (mongo/insert! (model-collection model) (client-keys model))
    "Error"))

(defn- update-by-modifiers
  [model]
  (let [bare (dissoc model :_id :id)]
    {:$set bare}))

(defmethod update-model! :default
  [model & id]
  (let [result
  (if id
    (if (and (objectid? (first id)) (valid-model-params? model))
      (.getError
       (mongo/update! (model-collection model)
		      {:_id (first id)}
		      (update-by-modifiers (client-keys model))
		      :upsert false))
      "Error")
    (if (and (model? model) (valid-model-params? model))
      (.getError
       (mongo/update! (model-collection model)
		      {:_id (:_id model)}
		      (update-by-modifiers (client-keys model))
		      :upsert false))
      "Error"))]
	(println result)
	result))
  
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

(defmulti serialize-client-object
;;  "Convert a server-side object to JSON for transmission to a backbone client"
  :type)

;;
;; Generic object translation methods
;;

(defn- add-db-ref [ns model key]
  (if-let [[type id] (model key)]
    (assoc model key
	   (mongo/db-ref (model-collection {:type type}) (mongo/object-id id)))
    model))

(defn- add-db-refs [model]
  (reduce add-db-ref model (db-reference-params model)))

(defmethod serialize-client-object :default
  [object]
  (cond
   (nil? object) nil
   (seq? object)
   (map serialize-client-object object)
   (associative? object)
   (add-db-refs
    (dissoc (assoc object :id (str (:_id object))) :_id))
   true object))

(defn deserialize-client-object
  "Take any properly formatted json object in parsed form and
   extract an object with an :id field and a :type field"
  [json]
  (assert (:type json))
  (if (and (:id json) (= (count (:id json)) 24))
    (dissoc (assoc json :_id (mongo/object-id (:id json))) :id)
    json))

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
