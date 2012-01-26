(ns experiment.models.comment
  (:use experiment.infra.models
        clodown.core)
  (:require [noir.validation :as vali]
            [experiment.infra.session :as session]
            [experiment.libs.datetime :as dt]))

;;
;; Discussion and comment machinery
;;


(defn parent-ref [id]
  (cond (and (string? id) (> (count id) 0)) (as-oid id)
        (objectid? id) id
        (or (nil? id) (= (count id) 0)) nil
        true (assert false)))

(defn order-by-date
  "Sort maps by :date (assume integer) field"
  [entries]
  (sort-by :date entries))

(defn assemble-topology [child-map obj]
  (if-let [children (child-map (:_id obj))]
    (assoc obj :children
           (order-by-date
            (map (partial assemble-topology child-map) children)))
    obj))

(defn topological-sort
  "Assemble tree from acyclic set of objects with :parent links via :_id"
  [objects]
  (let [child-map (group-by :parent objects)
        roots (child-map nil)]
    (map (partial assemble-topology child-map) roots)))
    
    
;; API

(defn all-comments [scope]
  (fetch-models :comment :where {:scope scope}))

(defn all-discussions
  "Get a topologically organized comment tree of nested
   comments with the given discussion scope"
  [scope]
  (-> (all-comments scope)
      topological-sort
      order-by-date))

(defn valid? [text]
  (vali/rule (vali/min-length? text 8)
             [:text "Comment is too short"]))

(defn comment! [scope parent text]
  (create-model!
   {:type :comment
    :scope scope
    :text text
    :html (md text)
    :owner (:username (session/current-user))
    :date (dt/as-utc (dt/now))
    :parent (parent-ref parent)}))

