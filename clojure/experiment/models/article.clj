(ns experiment.models.article
  (:use experiment.infra.models)
  (:require [clodown.core :as md]))

(defn get-article [name]
  (fetch-model :article {:name name}))
                  
(defn create-article! [name title body]
  (let [new {:type "article"
             :name (str name)
             :title title
             :body body
             :html (md/md body)}]
    (if-let [old (fetch-model :article {:name name})]
      (update-model! (merge old new))
      (create-model! (assoc new :type "article")))))

(defmethod update-model-hook :article [model]
  (assoc model :html (md/md (:body model))))