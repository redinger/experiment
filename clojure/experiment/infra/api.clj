(ns experiment.infra.api
  (:use noir.core
	experiment.infra.models)
  (:require
   [clojure.data.json :as json]
   [noir.response :as response]
   [noir.request :as request]))

;; -------------------------
;; Backbone Model CRUD API
;; -------------------------
;;
;; This package implements an interface for Backbone clients or
;; any front-end that wishes to manipulate Client-Server models.
;; This API leverages the ClientServer model api in
;; experiment.infra.models to transform objects and handle CRUD
;; operations.

;; Create - POST
(defpage backbone-api-create [:post "/api/bone/:type"]
  {:keys [type json-payload]}
  (response/json
   (server->client 
    (try
      (create-model! (new-client->server (assoc json-payload :type type)))
      (catch java.lang.Throwable t
	(response/status
         400
         (str "Error: Problem deserializing or saving new model " json-payload)))))))

;; Update - PUT
(defpage backbone-api-update [:put "/api/bone/:type"]
  {:keys [type json-payload]}
  (assert (sequential? json-payload))
  (response/json
   (server->client 
    (update-model! (map client->server json-payload)))))

(defpage backbone-api-update [:put "/api/bone/:type/:id"]
  {:keys [type id json-payload]}
  (let [model (client->server json-payload)]
    (assert (= (deserialize-id id) (:_id model)))
    (response/json
     (server->client 
      (update-model! model)))))

;; Read - GET
(defpage backbone-api-read-all [:get "/api/bone/:type"]
  {:keys [type options] :as params}
  (response/json
   (vec
    (map server->client
	 (apply fetch-models type (when options (json/read-json options true)))))))

(defpage backbone-api-read [:get "/api/bone/:type/:id"]
  {:keys [type id] :as params}
  (response/json
   (server->client
    (fetch-model type :where {:_id (deserialize-id id)}))))

;; Delete - DELETE
(defpage backbone-api-delete-id [:delete "/api/bone/:type/:id"]
  {:keys [type id]}
  (response/json
   (server->client
    (delete-model! {:type type :_id (deserialize-id id)}))))

(defpage backbone-api-delete-model [:delete "/api/bone/:type"]
  {:keys [type json-payload]}
  (response/json
   (server->client
    (delete-model! (client->server json-payload)))))

;; ----------------------------
;; Backbone SubModel CRUD API
;; ----------------------------

;; Create Submodel
;;
;; - Insert into path location in model
;; - Submodels must have a unique ID, they are $set into location+id
;; - Submodels adhere to client-server protocol?
;; - Submodels also use the :_id field for consistency
;;
(defpage backbone-sub-api-create [:post "/api/bones/:mtype/:mid/:location"]
  {:keys [mtype mid location json-payload] :as args}
  (let [parent (resolve-dbref mtype mid)]
    (response/json
     (when-let [submodel (and parent (new-client->server json-payload))]
       (create-submodel! parent location submodel)))))

;; Get Submodel at location + id
(defpage backbone-sub-api-read [:get "/api/bones/:mtype/:mid/:location/:id"]
  {:keys [mtype mid location id] :as args}
  (let [parent (resolve-dbref mtype mid)]
    (response/json
     (server->client
      (get-submodel parent (submodel-path location id))))))

;; Get All Submodels at Location
(defpage backbone-sub-api-read-all [:get "/api/bones/:mtype/:mid/:location"]
  {:keys [mtype mid location] :as args}
  (let [parent (resolve-dbref mtype mid)]
    (response/json
     (server->client
      (vals (get-submodel parent location))))))

;; Update Submodel 
(defpage backbone-sub-api-update [:put "/api/bones/:mtype/:mid/:location/:id"]
  {:keys [mtype mid location id json-payload] :as args}
  (response/json
   (set-submodel! {:type mtype :_id (deserialize-id mid)}
                  (submodel-path location id)
                  (client->server json-payload))))

;; Update all submodels (this overwrites everything)
;; NOTE: Does this do the right thing with submodel updates?
(defpage backbone-sub-api-update-all [:put "/api/bones/:mtype/:mid/:location"]
  {:keys [mtype mid location json-payload] :as args}
  (assert (and (sequential? json-payload) false)) ;; Test before allowing
  (response/json
   (server->client 
    (let [objects (zipmap (map :id json-payload) (map client->server json-payload))]
      (set-submodel! {:type mtype :_id (deserialize-id mid)}
                     location
                     objects)))))

;; Delete submodel
(defpage backbone-sub-api-delete-id [:delete "/api/bone/:type/:id"]
  {:keys [type id]}
  (response/json
   (server->client
    (delete-submodel! {:type type :_id (deserialize-id id)}))))

(defpage backbone-api-delete-model [:delete "/api/bone/:type"]
  {:keys [type json-payload]}
  (response/json
   (server->client
    (delete-submodel! (client->server json-payload)))))


;; -------------------------------------------
;; Miscellanious Model-related API calls
;; -------------------------------------------
;;
;; Deprecated; remove soon...
;;

(defpage backbone-api-annotate [:post "/api/annotate/:mtype/:mid/:location"]
  {:keys [mtype mid location json-payload] :as args}
  (let [object (resolve-dbref mtype mid)]
    (response/json
     (when-let [anno (and object (make-annotation json-payload))]
       (annotate-model! object location anno)
       true))))



