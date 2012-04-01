(ns experiment.views.help
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        [hiccup.form-helpers :exclude [form-to label]]
        experiment.infra.models
        experiment.models.user
        experiment.models.article
        experiment.views.bootstrap
        experiment.views.discuss)
  (:require
   [clojure.tools.logging :as log]
   [noir.response :as resp]
   [noir.request :as req]
   [experiment.infra.session :as session]
   [experiment.models.comment :as comment]
   [experiment.views.common :as common]
   [experiment.views.discuss :as discuss]))

;; Discussion page


(defpage "/help" {}
  (common/layout
   ["Help Page"
    (common/default-nav (common/nav-user-name))]
   [:div.container
    [:div.page-header
     [:h1 "Help for Personal Experiments"]
     [:p "This page provides a place for asking questions about personal experiments.  You must be a logged-in user to add new comments here"]]
    [:div.span8
     (discuss/discussions "help" "/help")]]))

(defpage [:post "/help"]
  {pid :id text :text :as data}
  (if (comment/valid? text)
    (do
      (comment/comment! "help" pid text)
      (render "/help"))
    (do
      (discuss/with-submission data
        (render "/help" data)))))
