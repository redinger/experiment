(ns experiment.infra.api
  (:require
   [noir.response :as response]
   [noir.request :as request]
   [somnium.congomongo :as mongo]
   [clojure.data.json :as json])
  (:use noir.core
	experiment.infra.models))

;;
;; Backbone Model Interaction API 
;;

;; Create - POST
(defpage backbone-api-create [:post "/api/bone/:type"] {:keys [type json-payload]}
  (println "Handling Post")
  (response/json
   (serialize-client-object
    (try
      (create-model! (assoc json-payload :type type))
      (catch java.lang.Throwable t
	(str "Error: Problem deserializing or saving new model " json-payload))))))
     

;; Update - PUT
(defpage backbone-api-update [:put "/api/bone/:type"] {:keys [type json-payload]}
;;  (println "Handling Put")
  (response/json
   (serialize-client-object 
    (update-model! (deserialize-client-object json-payload)))))

(defpage backbone-api-update [:put "/api/bone/:type/:id"] {:keys [type id json-payload]}
;;  (println "Handling Put w/ ID")
  (let [model (deserialize-client-object json-payload)]
;;    (println model)
    (assert (= (deserialize-id id) (:_id model)))
    (response/json
     (serialize-client-object 
      (update-model! model)))))

;; Read - GET
(defpage backbone-api-read-all "/api/bone/:type" {:keys [type options json-payload] :as params}
;;  (println params)
  (response/json
   (serialize-client-object 
    (let [models (apply fetch-models type (when options (json/read-json options true)))]
;;      (println "GET ALL: " models)
      (map client-keys models)))))

(defpage backbone-api-read "/api/bone/:type/:id" {type :type id :id}
  (response/json
   (serialize-client-object
    (let [model (fetch-model type :where {:_id (mongo/object-id id)})]
;;      (println "GET: " model)
      (client-keys model)))))


;; Delete - DELETE
(defpage backbone-api-delete-id [:delete "/api/bone/:type/:id"] {:keys [type id]}
;;  (println "Delete id")
  (response/json
   (serialize-client-object
    (delete-model! {:type type :_id (mongo/object-id id)}))))

(defpage backbone-api-delete-model [:delete "/api/bone/:type"] {:keys [type json-payload]}
;;  (println "Delete model")
  (response/json
   (serialize-client-object
    (delete-model! (deserialize-client-object json-payload)))))

