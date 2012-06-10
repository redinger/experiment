(ns experiment.views.article-page
  (:require
   [experiment.views.common :as common]
   [experiment.infra.session :as session]
   [clojure.tools.logging :as log]
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

(defpartial edit-link [name]
  (when (is-admin?)
    [:a.admin-link {:href (format "/article/edit/%s" name)}
     "Edit Article"]))

(defpage "/article/:name" {:keys [name] :as options}
  (let [article (get-article name)]
    (common/layout
     [(:title article)
      (common/default-nav name)]
     [:div.container
      (if article
        [:div.article.span8
         (edit-link name)
         [:div.page-header
          [:h1 (:title article)]]
         (:html article)
         (link-to (or (:target options) "/") "Return to Home Page...")]
        [:div.article.span8
         [:h1 "No Article named '" name "' found"]])])))

(defpage "/article/edit/:name" {:keys [name] :as options}
  (let [article (or (get-article name) {:name "" :title "" :body ""})]
    (common/layout
     [(str "Editing: " (:title article))
      (common/default-nav (if (= name "about") "About" ""))
      :deps ["views/home"]]
     [:div.container
      (form-to [:post "/article/edit"]
               [:fieldset
                [:div.control-group
                 (label {:class "control-label"} "name" "Database Label")
                 [:div.controls
                  (text-field {:class "input-xlarge"} "name" (:name article))]]
                [:div.control-group
                 (label {:class "control-label"} "title" "Article Title")
                 [:div.controls
                  (text-field {:class "input-xlarge"} "title" (:title article))]]
                [:div.control-group
                 (label {:class "control-label"} "body" "Article Content")
                 [:div.controls
                  [:p.help-block
                   "This box uses the " [:a {:href "http://daringfireball.net/projects/markdown/syntax"} "Markdown"]
                   " markup language."]
                  (text-area {:class "input-xlarge" :style "width:40em" :rows "30"}
                             "body" (:body article))
                  ]]
                [:div.form-actions
                 [:button.btn.btn-primary {:type "submit" :name "submit"} "Save"]
                 [:button.btn {:type "submit" :name "cancel"} "Cancel"]]])
      [:script "$(document).ready(function () { $('#body').focus(); });"]])))

(defpage [:post "/article/edit"] {:as article}
  (when (and (not (:cancel article))
             (:name article) (:title article) (:body article))
    (create-article! (:name article) (:title article) (:body article)))
  (resp/redirect (format "/article/%s" (:name article))))
  