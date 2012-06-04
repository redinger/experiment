(ns experiment.libs.fulltext
  (:use experiment.infra.models)
  (:require [clucy.core :as clucy]
            [clojure.tools.logging :as log]))

;; # Setup

(def index-filename "db/fulltext")
(defonce db nil)

(defn start [& [filename]]
  (if db
    (clojure.tools.logging/warn "Fulltext index already open, ignoring start")
    (do
      (when filename
        (alter-var-root #'index-filename (fn [old] filename)))
      (alter-var-root #'db (fn [old] (clucy/disk-index index-filename))))))

;; # Indexing Models

(defn- index-model [model]
  (try
    (when-let [keys (index-keys model)]
      (select-keys model (conj keys :_id :type)))
    (catch java.lang.IllegalArgumentException e
      (clojure.tools.logging/warnf "Model type %s does not have index-keys defined" (:type model))
      nil)))
  
(defn index [model & {:as extras}]
  (assert (and db (map? model) (:type model)))
  (let [mod (merge (index-model model) extras)]
    (if (not (empty? mod))
      (do
        (clucy/search-and-delete db (str "_id:" (:_id mod)))
        (clucy/add
         db
         (with-meta mod
           (assoc {}
             ;; (into {} (map (fn [[field val]]lt
             ;;         {field {:stored false}})
             ;;       mod))
             :_id {:tokenized false}
             :type {:tokenized false}))))
      (log/infof "Cannot index model type: %s" (:type model)))))

(defn delete [model]
  (clucy/search-and-delete db (str "_id:" (:_id model))))

(defn index-all [& [collections]]
  (let [colls (or collections ["instrument" "treatment" "experiment"])]
    (doseq [coll colls]
      (doall (map index (fetch-models coll))))))

(defn reindex-all [collections]
  (doseq [coll collections]
    (clucy/search-and-delete db (str "type:" (name coll))))
  (index-all collections))

;; # Query for Models

(defn- as-query-string [qmap]
  (let [dq (:default qmap)]
    (apply str (cons
                (if (and dq (> (count dq) 0))
                  (str "+( " dq " ) ")
                  "")
                (map (fn [[field string]]
                       (str "+" (name field) ":( " string  " ) "))
                     (dissoc qmap :default))))))

(defn resolve-group [[type refs]]
  (fetch-models (model-collection type)
                {:_id {:$in (vec (map (comp as-oid :_id) refs))}}))
  
(defn resolve-refs [refs]
  (->> refs
       (group-by :type)
       (mapcat resolve-group)))
  

(defn search [query-map & {:keys [max-results size skip]}]
  (let [query (as-query-string query-map)
;;        _ (println query)
        results (clucy/search db query
                              (or max-results (and (not skip) size) 200))
        {:keys [_total-hits]} (meta results)]
    {:hits _total-hits
;;     :results results
     :models (resolve-refs
              (if (and size skip)
                (take size (drop skip results))
                results))}))
      
  