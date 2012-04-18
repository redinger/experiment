(ns experiment.infra.api
  (:use noir.core
	experiment.infra.models)
  (:require
   [clojure.data :as data]
   [clojure.string :as str]
   [cheshire.core :as json]
   [noir.response :as response]
   [noir.request :as request]))

;; ## Be clear about what methods are content pages and which are APIs

(defmacro defapi [name spec params & body]
  `(defpage ~name ~spec ~params
     ~@(if (> (count body) 1)
         (concat (butlast body) `((response/json ~(last body))))
         `((response/json ~@body)))))

;;
;; Utilities
;;

(defn- update-and-diff [new-model]
  (let [signature (select-keys new-model [:_id :type])
        old-model (fetch-model signature)]
    (map (partial merge signature)
         (take 2 (data/diff new-model old-model)))))

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
     (create-model! (new-client->server (assoc json-payload :type type)))
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
        _ (assert (= (deserialize-id id) (:_id new-model)))
        [to-update to-return] (update-and-diff new-model)]
    (let [result (update-model! to-update)]
      (if (string? result) ;; error?
        {}
        (server->client to-return)))))

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
  (delete-model! {:type type :_id (deserialize-id id)}))

(defapi backbone-api-delete-model [:delete "/api/root/:type"]
  {:keys [type json-payload]}
  (delete-model! (client->server json-payload)))


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
       (create-submodel! parent location submodel)))))

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
  (let [location (submodel-location *)]
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



