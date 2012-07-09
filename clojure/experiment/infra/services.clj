(ns experiment.infra.services
  (:refer-clojure :exclude [get set])
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [experiment.infra.models :as models]))

(defonce services (atom nil))

(def reserved-properties #{:name :schema :fields :options :oauth})

(defn reserved-property? [property]
  (reserved-properties (keyword property)))

(defn strip-service-properties [record]
  (apply dissoc record reserved-properties))

;;
;; Register services with user-configured fields
;;

(defn- add!
  "Internal method to update service list"
  [tag service]
  (swap! services (fn [old] (merge old {tag service}))))

(defn register
  "Stylized interface for registring a service description"
  [tag [name & {:as options}] & args]
  (assert (and name (> (count args) 1)))
  (let [schema (apply sorted-map args)
        fields (keys schema)]
    (add! tag
          (merge {:name name :options options}
                 {:schema schema}
                 {:fields fields}))))

(defn register-oauth
  "Register an oauth service. No schema, instead oauth URL"
  [tag [name & {:as options}]
   & {:keys [title url] :as config}]
  (add! tag (merge {:name name :options options :oauth true}
                   config)))

(defn get-config [tag]
  "Retrieve the service description for a service by tag"
  (if (keyword? tag)
    (tag @services)
    (@services tag)))

(defn render-registry
  "Render a JSON script tag with all service descriptions embedded in an object prefixed
   by their tag type"
  []
  [:script {:type "text/x-json" :id "services-registry"}
   (json/generate-string
    @services)])

;;
;; Update user object service entry (e.g. oauth creds)
;;

(defn get [user tag property]
  (get-in user [:services (keyword tag) (keyword property)]))

(defn set [user tag property value]
  (assert (not (reserved-property? property)))
  (assoc-in user [:services (keyword tag) (keyword property)] value))

(defn set-model!
  "Modify MongoDB directly (convenience).  Overwrites existing."
  [user tag map]
  (let [prefix (str "services." (name tag))
        model (assoc map :id tag)]
    (models/modify-model!
     user
     {:$set {prefix model}
      :$inc {"updates" 1}})))

;;
;; Service Models
;;

;; Don't expose oauth data to network!
(defmethod models/public-keys :service [svc]
  (keys (dissoc svc :oauth)))

(defmethod models/import-keys :service [svc]
  (keys svc))

