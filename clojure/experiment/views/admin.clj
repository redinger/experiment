(ns experiment.views.admin
  (:use noir.core
	hiccup.core
	hiccup.page-helpers
	hiccup.form-helpers)
  (:require
   [noir.session :as session]
   [noir.validation :as vali]
   [noir.response :as resp]
   [clojure.string :as string]
   [experiment.models.user :as users]
   [experiment.views.common :as common]))

;;(pre-route "/admin*" {}
;;           (when-not (users/admin?)
;;             (resp/redirect "/actions/login")))

