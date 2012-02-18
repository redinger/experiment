(ns experiment.views.article-page
  (:require
   [experiment.views.common :as common]
   [experiment.infra.session :as session]
   [clojure.tools.logging :as log]
   [clojure.data.json :as json]
   [clojure.string :as str]
   [somnium.congomongo :as mongo]
   [noir.response :as resp]
   [noir.util.crypt :as crypt])
  (:use noir.core
	experiment.infra.models
        experiment.models.article
        experiment.models.user
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers))

(defpage "/article/:name" {:keys [name] :as options}
  (let [article (get-article name)]
    (common/simple-layout {}
     (if article
       [:div.article
        (link-to (or (:target options) "/") "Return to Home Page...")
        (when (is-admin?)
          [:a.admin-link {:href (format "/article/edit/%s" name)}
           "Edit Article"])
	[:h1 (:title article)]
	(:html article)]
       [:div#main
	[:h1 "No Article named '" name "' found"]]))))

(defpage "/article/edit/:name" {:keys [name] :as options}
  (let [article (get-article name)]
    (common/simple-layout {}
      (form-to [:post "/article/edit"]
               [:div.form-field
                [:span.head (label "name" "Name")]
                (text-field "name" (:name article))]
               [:div.form-field
                [:span.head (label "title" "Title")]
                (text-field "title" (:title article))]
               [:div.form-field
                [:span.head (label "body" "Body")]
                (text-area "body" (:body article))]
               [:input.form-button {:type "submit" :name "submit" :value "Submit"}]
               [:input.form-button {:class "cancel-button" :type "submit" :name "cancel" :value "Cancel"}]))))

(defpage [:post "/article/edit"] {:as article}
  (when (and (not (:cancel article))
             (:name article) (:title article) (:body article))
    (create-article! (:name article) (:title article) (:body article)))
  (resp/redirect (format "/article/%s" (:name article))))
  