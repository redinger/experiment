(ns experiment.views.home
  (:require
   [experiment.views.common :as common]
   [experiment.infra.session :as session]
   [clojure.data.json :as json]
   [somnium.congomongo :as mongo]
   [noir.response :as resp]
   [noir.util.crypt :as crypt])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers))

;;
;; Ship a single app template for client side
;; application execution
;;

(defpartial app-skeleton []
  [:div#browserApp]
  [:div#homeApp]
  [:div#adminApp]
  [:div#reviewApp])

(defpartial share-skeleton []
  [:div#social])

(defpage "/app*" {}
  (common/app-layout
   [["home" "Home"]
    ["experiment" "Experiments"
     ["experiment1" "Experiment1"]
     ["experiment2" "Experiment2"]
     ["past" "Past Experiments"]]
    ["browse" "Browse"]
    (when (= (:username (session/user)) "eslick")
      ["/admin" "Admin"])]
   (app-skeleton)
   (share-skeleton)))
     
