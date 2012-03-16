(ns experiment.models.comment
  (:use experiment.infra.models
        clodown.core)
  (:require
   [clojure.tools.logging :as log]
   [noir.validation :as vali]
   [experiment.libs.datetime :as dt]
   [experiment.libs.mail :as mail]
   [experiment.infra.session :as session]
   [experiment.models.user :as user]))

;;
;; Reddit style discussion forum machinery
;; ------------------------------------
;;
;; TODO:
;; -  Add support for ranking/sorting (quora like)
;; -  Add support for collapsing long chains


;; ## Utilities

(defn parent-ref [id]
  (cond (and (string? id) (> (count id) 0)) (as-oid id)
        (objectid? id) id
        (or (nil? id) (= (count id) 0)) nil
        true (assert false)))

(defn- parent-model [comment]
  (when-let [parent (:parent comment)]
    (safe-resolve-dbref :comments parent)))

(defn order-by-date
  "Sort maps by :date (assume integer) field"
  [entries]
  (sort-by :date entries))

(defn all-comments [scope]
  (fetch-models :comment {:scope scope}))

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

(defn thread-root [comment]
  (if-let [parent (parent-model comment)]
    (thread-root parent)
    comment))

(defn flatten-tree [children-key tree]
  (reduce concat (list tree)
          (map (partial flatten-tree children-key)
               (children-key tree))))

(defn thread-tree
  "NOTE: This is more expensive than it needs be, but is simple to
   reason about"
  [root-comment]
  (let [models (all-comments (:scope root-comment))
        topo (topological-sort models)]
    (first (filter #(= (:_id root-comment) (:_id %)) topo))))

(defn thread-users
  "Get all posters to a thread of which this comment is a part,
   exclude the author of the comment."
  [comment]
  (->> (thread-root comment)
       thread-tree 
       (flatten-tree :children)
       (map (comp user/get-user :owner))))

(defn comment-id
  "Most significant 5 chars of a random hash should be unique
   with high probability (this is a git hack used to name patches)"
  [comment]
  (.substring (str (:_id comment)) 0 5))

(defn comment-url [comment]
  (case (:scope comment)
    "study1" (str "http://personalexperiment.org/study1/discuss#" (comment-id comment))
    "help" (str "http://personalexperiments.org/help#" (comment-id comment))))
           
;; ## Notifications
   
(defn comment-notification-subject [comment]
  (str
   "New Comment on Personal Experiments"))
  
(defn comment-notification-body [comment]
  (let [{:keys [owner parent text]} comment
        root (thread-root comment)
        url (comment-url comment)]
    (str
     "User '" owner "' posted a new response"
     " on Personal Experiments:\n"
     "\n---------------------------------------\n"
     (:text comment)
     "\n---------------------------------------\n"
     "\nto the original question:\n"
     "\n---------------------------------------\n"
     (:text root)     
     "\n---------------------------------------\n"
     "\nView the comment thread here: " url
     "\n\nYou have received this comment because you are a registered user of Personal Experiments and have posted a question or response on the site.")))
   
           
(defn send-comment-notifications
  "Send notifications to the site owner and all posters in the
   specific thread"
  [comment]
  (try
    (let [message {:subject (comment-notification-subject comment)
                   :body (comment-notification-body comment)}
          emails (distinct (map :email (thread-users comment)))]
      (mail/send-message-to-group message (concat "ianeslick@gmail.com" emails)))
    (catch java.lang.Throwable e
      (log/error e "Error sending notifications")
      nil)))

;; API

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
  (let [username (:username (session/current-user))
        comment {:type :comment
                 :scope scope
                 :text text
                 :html (md text)
                 :owner username
                 :date (dt/as-utc (dt/now))
                 :parent (parent-ref parent)}
        result (create-model! comment)]
    (send-comment-notifications comment)
    result))

