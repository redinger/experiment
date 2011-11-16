(ns experiment.infra.api
  (:use noir.core
	experiment.infra.models)
  (:require
   [noir.response :as response]
   [noir.request :as request]
   [somnium.congomongo :as mongo]
   [clojure.data.json :as json]))

;;
;; Backbone Model Interaction API 
;;

;; Create - POST
(defpage backbone-api-create [:post "/api/bone/:type"] {:keys [type json-payload]}
  (println "Handling Post")
  (response/json
   (export-model 
    (try
      (create-model! (import-new-model (assoc json-payload :type type)))
      (catch java.lang.Throwable t
	(str "Error: Problem deserializing or saving new model " json-payload))))))
     

;; Update - PUT
(defpage backbone-api-update [:put "/api/bone/:type"] {:keys [type json-payload]}
;;  (println "Handling Put")
  (assert (sequential? json-payload))
  (response/json
   (export-model 
    (update-model! (map import-model json-payload)))))

(defpage backbone-api-update [:put "/api/bone/:type/:id"] {:keys [type id json-payload]}
;;  (println "Handling Put w/ ID")
  (let [model (import-model json-payload)]
    (assert (= (deserialize-id id) (:_id model)))
    (response/json
     (export-model 
      (update-model! model)))))

;; Read - GET
(defpage backbone-api-read-all "/api/bone/:type" {:keys [type options json-payload] :as params}
  (println "GET " params)
  (response/json
   (vec
    (map export-model
	 (apply fetch-models type (when options (json/read-json options true)))))))
      

(defpage backbone-api-read "/api/bone/:type/:id" {:keys [type id] :as params}
  (println "GET ONE " params)
  (response/json
   (export-model
    (fetch-model type :where {:_id (mongo/object-id id)}))))


;; Delete - DELETE
(defpage backbone-api-delete-id [:delete "/api/bone/:type/:id"] {:keys [type id]}
  (println "Delete id")
  (response/json
   (export-model
    (delete-model! {:type type :_id (mongo/object-id id)}))))

(defpage backbone-api-delete-model [:delete "/api/bone/:type"] {:keys [type json-payload]}
  (println "Delete model")
  (response/json
   (export-model
    (delete-model! (import-model json-payload)))))

;; Convenience method for annotating major objects with subobjects

(defpage backbone-api-annotate [:post "/api/annotate/:mtype/:id/:type"]
  {:keys [mtype id type] :as args}
  (let [object (resolve-dbref mtype id)]
    (response/json
     (when-let [anno (and object (make-annotation (dissoc args :mtype :id)))]
       (annotate-model! object (model-collection {:type type}) anno)
       true))))
			
			


