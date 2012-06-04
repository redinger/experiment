(ns experiment.models.profile
  (:use experiment.infra.models
        noir.core
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers
	handlebars.templates)
  (:require [experiment.libs.datetime :as dt]
            [experiment.infra.session :as session]))


(defn timezone-list []
  [["AST -4" "ast"]
   ["PST -5" "pst"]
   ["MST -6" "mst"]
   ["CST -7" "cst"]
   ["EST -8" "est"]])

;; Internal accessors

(defn get-user-profile [user]
  (:profile user))
    
(defn set-user-profile!
  ([user profile]
     (set-submodel! user :profile profile)))

