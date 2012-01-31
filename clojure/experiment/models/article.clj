(ns experiment.models.article
  (:use experiment.infra.models)
  (:require [clodown.core :as md]))

(defn get-article [name]
  (fetch-model :article :where {:name name}))
                  
(defn create-article! [name title body]
  (let [new {:type "article"
             :name (str name)
             :title title
             :body body}]
    (if-let [old (fetch-model :article :where {:name name})]
      (update-model! (merge old new))
      (create-model! (assoc new :type "article")))))

(defmethod update-model-pre "article" [model]
  (assoc model :html (md/md (:body model))))