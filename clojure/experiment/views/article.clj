(ns experiment.views.article
  (:require
   [experiment.views.common :as common]
   [experiment.infra.session :as session]
   [clojure.data.json :as json]
   [clojure.string :as str]
   [somnium.congomongo :as mongo]
   [noir.response :as resp]
   [noir.util.crypt :as crypt])
  (:use noir.core
	experiment.infra.models
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers))

(defn format-article [text]
  (str/replace text #"\n" "<br>"))

(defpage "/article/:name" {:keys [name]}
  (let [article (fetch-model :article :where {:name name})]
    (common/simple-layout
     (if article
       [:div
	[:h1 (:title article)]
	(if (session/logged-in?)
	  (link-to "/app/dashboard" "Return to Home Page...")
	  (link-to "/" "Return to Home Page..."))
	[:br][:br]
	(format-article (:body article))]
       [:div#main
	[:h1 "No Article named '" name "' found"]]))))
