(ns experiment.models.trial
  (:use
   experiment.infra.models
   experiment.models.user)
  (:require
   [experiment.libs.datetime :as dt]))

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

(defn trial-user [trial]
  (resolve-dbref (:user trial)))

(defn trial-trackers [trial]
  (let [ids (:tracker-ids trial)
        trackers (:trackers (trial-user trial))]
    (map #((keyword %) trackers) ids)))

(defn reminder-events [trial interval]
;; TODO
  nil)

(defn all-reminder-events [user interval]
  (mapcat #(reminder-events % interval) (trials user)))
  