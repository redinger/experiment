(ns experiment.infra.api
  (:use noir.core
	experiment.infra.models)
  (:require
   [cheshire.core :as json]
   [noir.response :as response]
   [noir.request :as request]))

;; ## Be clear about what methods are content pages and which are APIs

(defmacro defapi [name spec params & body]
  `(defpage ~name ~spec ~params
     ~@(if (> (count body) 1)
         (concat (butlast body) `((response/json ~(last body))))
         `((response/json ~@body)))))

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
(defapi backbone-api-create [:post "/api/bone/:type"]
  {:keys [type json-payload]}
  (server->client 
   (try
     (create-model! (new-client->server (assoc json-payload :type type)))
     (catch java.lang.Throwable t
       (response/status
        400
        (str "Error: Problem deserializing or saving new model " json-payload))))))

;; Update - PUT
(defapi backbone-api-update [:put "/api/bone/:type"]
  {:keys [type json-payload]}
  (assert (sequential? json-payload))
  (server->client 
   (update-model! (map client->server json-payload))))

(defapi backbone-api-update [:put "/api/bone/:type/:id"]
  {:keys [type id json-payload]}
  (let [model (client->server json-payload)]
    (assert (= (deserialize-id id) (:_id model)))
    (server->client 
     (update-model! model))))

;; Convenience for setting kv-pairs at an interior point in a document
(defn modify-response [write]
  (let [cmdRes (.getCachedLastError write)]
    (if (.ok cmdRes)
      {:result "success"}
      {:result "error" :message (.getErrorMessage cmdRes)})))

(defapi backbone-api-update-path [:put "/api/bone/:type/:id/:path"]
  {:keys [type id path json-payload]}
  (modify-response
   (modify-model! {:type type :_id id}
                  (update-by-modifiers json-payload path))))
                   
;; Read - GET
(defapi backbone-api-read-all [:get "/api/bone/:type"]
  {:keys [type options] :as params}
  (vec
   (map server->client
        (apply fetch-models type (when options (json/parse-string options true))))))

(defapi backbone-api-read [:get "/api/bone/:type/:id"]
  {:keys [type id] :as params}
  (server->client
   (fetch-model type :where {:_id (deserialize-id id)})))

;; Delete - DELETE
(defapi backbone-api-delete-id [:delete "/api/bone/:type/:id"]
  {:keys [type id]}
  (server->client
   (delete-model! {:type type :_id (deserialize-id id)})))

(defapi backbone-api-delete-model [:delete "/api/bone/:type"]
  {:keys [type json-payload]}
  (server->client
   (delete-model! (client->server json-payload))))

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
(defapi backbone-sub-api-create [:post "/api/embed/:mtype/:mid/:location"]
  {:keys [mtype mid location json-payload] :as args}
  (let [parent (resolve-dbref mtype mid)]
    (when-let [submodel (and parent (new-client->server json-payload))]
      (create-submodel! parent location submodel))))

;; Get Submodel at location + id
(defapi backbone-sub-api-read [:get "/api/embed/:mtype/:mid/:location/:id"]
  {:keys [mtype mid location id] :as args}
  (let [parent (resolve-dbref mtype mid)]
    (server->client
     (get-submodel parent (submodel-path location id)))))

;; Get All Submodels at Location
(defapi backbone-sub-api-read-all [:get "/api/embed/:mtype/:mid/:location*"]
  {:keys [mtype mid location] :as args}
  (let [parent (resolve-dbref mtype mid)]
    (server->client
     (vals (get-submodel parent location)))))

;; Update Submodel 
(defapi backbone-sub-api-update [:put "/api/embed/:mtype/:mid/:location*"]
  {:keys [mtype mid location id json-payload] :as args}
  (set-submodel! {:type (:type json-payload) :_id (deserialize-id mid)}
                 (submodel-path location id)
                 (client->server json-payload)))

;; Update all submodels (this overwrites everything)
;; NOTE: Does this do the right thing with submodel updates?
(defapi backbone-sub-api-update-all [:put "/api/embed/:mtype/:mid/:location*"]
  {:keys [mtype mid location json-payload] :as args}
  (assert (and (sequential? json-payload) false)) ;; Test before allowing
  (server->client 
   (let [objects (zipmap (map :id json-payload) (map client->server json-payload))]
     (set-submodel! {:type (:type json-payload) :_id (deserialize-id mid)}
                    location
                    objects))))

;; Delete submodel
(defapi backbone-sub-api-delete-id [:delete "/api/bone/:type/:id"]
  {:keys [type id]}
  (server->client
   (delete-submodel! {:type type :_id (deserialize-id id)})))

(defapi backbone-api-delete-model [:delete "/api/bone/:type"]
  {:keys [type json-payload]}
  (server->client
   (delete-submodel! (client->server json-payload))))




;; -------------------------------------------
;; Miscellanious Model-related API calls
;; -------------------------------------------
;;
;; Deprecated; remove soon...
;;

(defapi backbone-api-annotate [:post "/api/annotate/:mtype/:mid/:location"]
  {:keys [mtype mid location json-payload] :as args}
  (let [object (resolve-dbref mtype mid)]
    (when-let [anno (and object (make-annotation json-payload))]
      (annotate-model! object location anno)
      true)))



