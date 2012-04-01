(ns experiment.models.trial
  (:use experiment.infra.models)
  (:require [experiment.libs.datetime :as dt]))

;; ===========================================================
;; TRIAL
;;  refs User
;;  refs Experiment
;;  has outcome #[notstart, abandon, success, fail, uncertain]
;;  log [action ...] #[start, pause, unpause, end]
;;  schedule []
;;  

(defmethod db-reference-params :trial [model]
  [:experiment])

(defmethod public-keys :trial [model]
  [:_id :type :user :experiment
   :schedule :status])

(defn human-status [trial]
  ({:active "Active"
    :paused "Paused"
    :abandoned "Abandoned"
    :completed "Completed"
    nil "Unknown"}
   (keyword (:status trial))))

(defn trial-done? [trial]
  (when (#{:abandoned :completed} (:status trial)) true))
	     
(defmethod server->client-hook "trial" [trial]
  (assoc trial
    :status_str (human-status trial)
    :stats {:elapsed 21
	    :remaining 7
	    :intervals 1}
    :start-str (dt/as-short-date (:start trial))
    :status (human-status trial)
    :donep (trial-done? trial)
    :end-str (when-let [end (:end trial)] (dt/as-short-string end))))


  