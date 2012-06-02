(ns experiment.infra.api
  (:use noir.core
	experiment.infra.models)
  (:require
   [clojure.data :as data]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [cheshire.core :as json]
   [noir.response :as response]
   [noir.request :as request]
   [experiment.libs.fulltext :as ft]))

;; ## Be clear about what methods are content pages and which are APIs

(defn add-meta-status [resp result]
  (assoc resp :status (or (:status (meta result)) 200)))

(defmacro defapi [name spec params & body]
  `(defpage ~name ~spec ~params
     (let [result# (do ~@body)]
       (-> (response/json result#)
           (experiment.infra.api/add-meta-status result#)))))

;;
;; Utilities
;;

;; Convenience for setting kv-pairs at an interior point in a document
(defn modify-response [write]
  (let [cmdRes (.getCachedLastError write)]
    (if (.ok cmdRes)
      {:result "success"}
      {:result "error" :message (.getErrorMessage cmdRes)})))

(defn- submodel-location [string]
  (map keyword (str/split string #"/")))

(defn- root-signature [type id]
  {:type type :_id (deserialize-id id)})

;; -------------------------
;; Backbone Model CRUD API
;; -------------------------
;;
;; This package implements an interface for Backbone clients or
;; any front-end that wishes to manipulate Client-Server models.
;; This API leverages the ClientServer model api in
;; experiment.infra.models to transform objects and handle CRUD
;; operations.

;; ## Create - POST
(defapi backbone-api-create [:post "/api/root/:type"]
  {:keys [type json-payload]}
  (server->client 
   (try
     (doto (create-model! (new-client->server (assoc json-payload :type type)))
       ft/index)
     (catch java.lang.Throwable t
       (response/status
        400
        (str "Error: Problem deserializing or saving new model " json-payload))))))

;; ## Update - PUT
;;
;; The payload consists of :id, :type and all new or changed
;; fields - but it can consist of all fields too.  The new or
;; changed fields are saved over the existing model; differences
;; on the server side (say, implemented by 'hook', are returned
;; to the client.

(defapi backbone-api-update-model [:put "/api/root/:type/:id"]
  {:keys [type id json-payload]}
  (let [new-model (client->server json-payload)
        result (update-model! new-model)]
    (if (string? result) ;; error?
      {}
      (server->client
       (doto (fetch-model (:type new-model) {:_id (:_id new-model)})
         ft/index)))))

;; Read - GET
(defapi backbone-api-read-all [:get "/api/root/:type"]
  {:keys [type options] :as params}
  (vec
   (map server->client
        (apply fetch-models type (when options (json/parse-string options true))))))

(defapi backbone-api-read [:get "/api/root/:type/:id"]
  {:keys [type id] :as params}
  (server->client
   (fetch-model type {:_id (deserialize-id id)})))

;; Delete - DELETE
(defapi backbone-api-delete-id [:delete "/api/root/:type/:id"]
  {:keys [type id]}
  (doto {:type type :_id (deserialize-id id)}
    ft/delete
    delete-model!))

(defapi backbone-api-delete-model [:delete "/api/root/:type"]
  {:keys [type json-payload]}
  (doto (client->server json-payload)
    ft/delete
    delete-model!))


;; ----------------------------
;; Backbone SubModel CRUD API
;; ----------------------------

;; Create Submodel
;;
;; - Submodels adhere to client-server protocol
;; - Submodels must have type for dispatch
;; - Submodels must have a unique ID, used to construct path inside parent model
;; - Submodels use the :id field instead of :_id indicating they are not stored
;;   directly in Mongo.
;; - Submodel IDs are SHA256 strings prefixed by SM for visual clarity

;; ### CREATE Submodel - add a submodel to embedded collection
;;
;; Since it is the temporary model doing the POST, we use the
;; instance embed API, but without the ID
;;
(defapi backbone-sub-api-create [:post "/api/embed/:mtype/:mid/*"]
  {:keys [mtype mid * json-payload] :as args}
  (let [parent (resolve-dbref mtype mid)
        location (submodel-location *)]
    (when-let [submodel
               (and parent
                    (new-client->server
                     (assoc json-payload
                       :submodel true)))]
      (server->client
       (let [model (create-submodel! parent location submodel)]
         (ft/index model)
         model)))))

;; ### GET Submodel 
(defapi backbone-sub-api-read [:get "/api/embed/:mtype/:mid/*"]
  {:keys [mtype mid *] :as args}
  (let [location (submodel-location *)]
    (server->client
     (get-submodel (root-signature mtype mid) location))))

(defapi backbone-sub-api-read-all [:get "/api/embed/coll/:mtype/:mid/*"]
  {:keys [mtype mid *] :as args}
  (let [location (submodel-location *)]
    (server->client
     (vals (get-submodel (root-signature mtype mid) location)))))

;; ### UPDATE Submodel 
(defapi backbone-sub-api-update [:put "/api/embed/:mtype/:mid/*"]
  {:keys [mtype mid * id json-payload] :as args}
  (let [location (submodel-location *)
        submod (client->server (assoc json-payload :submodel true))]
    (set-submodel! (root-signature mtype mid) location
                   (client->server (assoc json-payload :submodel true)))
    (server->client
     (get-submodel (root-signature mtype mid) location))))

;; ### DELETE Submodel
(defapi backbone-sub-api-delete-id [:delete "/api/embed/:mtype/:mid/*"]
  {:keys [mtype mid *]}
  (let [location (submodel-location *)]
    (do (delete-submodel! (root-signature mtype mid) location)
        true)))



