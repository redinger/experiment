(ns experiment.views.admin
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers)
  (:require
   [noir.validation :as vali]
   [noir.response :as resp]
   [clojure.string :as string]
   [experiment.models.user :as users]
   [experiment.infra.session :as sess]
   [experiment.views.common :as common]))

(pre-route
 "/admin*" {}
 (if (sess/current-user)
   (when-not (users/is-admin?)
     (resp/redirect "/"))
   (resp/redirect "/actions/login")))

(defpage "/admin/hideme" {}
  (html5
   (common/layout-header "Personal Experiments: Admin")
   (:body {:onLoad "javascript:pageTracker._setVar('dev_view');"}
          [:h1 "You are now hidden from Google Analytics"])))



