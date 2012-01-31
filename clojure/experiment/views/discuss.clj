(ns experiment.views.discuss
  (:use experiment.infra.models
        noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers)
  (:require
   [clojure.string :as str]
   [noir.validation :as vali]
   [clodown.core :as md]
   [experiment.libs.datetime :as dt]
   [experiment.infra.session :as session]
   [experiment.models.comment :as comment]))

;; Active forms

(def ^:dynamic *submitted-data* nil)

(defmacro with-submission [data & body]
  `(binding [*submitted-data* ~data]
     ~@body))

(defn active-form? [id]
  (and *submitted-data*
       (or (= id (:id *submitted-data*))
           (and (nil? id) (= (count (:id *submitted-data*)) 0)))))
           

(defn submitted-text []
  (:text *submitted-data*))

;; Partials

(defpartial error-text [errors]
  [:p.error (str/join "<br/>" errors)])

(defpartial comment-fields [id]
  (when (active-form? id)
    (vali/on-error :text error-text))
  (if (active-form? id)
    (text-area :text (submitted-text))
    (text-area {:placeholder "Comment"} :text))
  (hidden-field :id id))

(defn form-class [id]
  (if (active-form? id)
    "comment-form"
    "hidden comment-form"))

(defn comment-form [base link-title current-id]
  [:div
   [:a.show-dform {:href ""} (or link-title "Reply")]
   [:div {:class (form-class current-id)}
    (form-to [:post base]
             (comment-fields current-id)
             [:br]
             (submit-button "Add comment"))]])

(defn comment-body [comment]
  (let [{date :date owner :owner html :html} comment]
    [:div.discussion-body
     [:span.discussion-byline
      "Posted by " [:b owner] " at " (dt/as-short-string
                                      (dt/to-default-tz
                                       (dt/from-utc date)))]
     html]))

(defn discussion-class [level]
  (format "discussion discussion-%d" level))

(defn discussion-thread
  ([base discussion level]
     [:div {:class (discussion-class level)}
      (comment-body discussion)
      (when (session/current-user)
        (comment-form base "Reply" (str (:_id discussion))))
      (map #(discussion-thread base % (+ level 1)) (:children discussion))
      (if (= level 0) [:hr])])
  ([base discussion]
     (discussion-thread base discussion 0)))

(defpartial discussions [scope base]
  (let [discussions (comment/all-discussions scope)]
    (list
     [:h1 "Questions, Answers and Comments"]
     (if (empty? discussions)
       [:h3 "No discussions found"]
       (map (partial discussion-thread base) discussions))
     (if (session/current-user)
       (comment-form base "New Discussion" nil)
       [:h3 "You must be logged-in to add comments"]))))
   
    

  