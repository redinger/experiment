(ns experiment.libs.zeo
  (:use experiment.infra.models)
  (:require [clj-http.client :as http]
            [experiment.libs.properties :as props]
            [experiment.infra.services :as services]
            [experiment.libs.datetime :as dt]))

(services/register
 :zeo
 ["Zeo"
  :description "Download data from the Zeo service"]
 :email {:title "Account Email"}
 :password {:title "Password" :type "Password"})
 
;; "ACE41D854610E84DAF16419E087C2ADF" ;; mit.edu
;; "6B58F54966A8A9632A68EBBFF0192D4C" ;; media.mit.edu

(defonce ^{:dynamic true} *auth* nil)

(defn- zeo-key []
  "F0F751291B8AFA0EC1BCC471A9A3B39D")
;; "ACE41D854610E84DAF16419E087C2ADF")
;; "6B58F54966A8A9632A68EBBFF0192D4C");;(props/get :zeo.key))

(defn- zeo-base []
  (props/get :zeo.url))

(defn- zeo-url [action]
  (format (zeo-base) action))

(defn valid-auth? [auth]
  (and (sequential? auth)
       (= (count auth) 2)
       (every? string? auth)))

(defn set-default-auth [auth]
  (assert (valid-auth? auth))
  (alter-var-root #'*auth* (fn [orig] auth)))
  
(defn get-default-auth []
  *auth*)

(defmacro with-auth [auth & body]
  `(binding [*auth* ~auth]
     ~@body))

(defn zeo-request
  ([auth action params]
     (assert (valid-auth? auth))
     (assert (map? params))
     (http/get (zeo-url action)
               {:as :json
                :debug true
                :query-params (assoc params :key (zeo-key))
                :basic-auth (or auth (get-default-auth))
                :content-type :json
                :accept :json}))
  ([action params]
     (zeo-request (get-default-auth) action params))
  ([action]
     (zeo-request (get-default-auth) action {})))

(defn zeo-date [date]
  (cond org.joda.time.DateTime
        (dt/as-iso-8601-date date)
        (string? date)
        (do (assert (re-matches #"(\d\d\d\d)-(\d\d)-(\d\d)" date))
            date)))

;;
;; These methods require default or dynamic *auth* setting
;;

(defn average-zq []
  (zeo-request "getOverallAverageZQScore"))

(defn average-morning-feel []
  (zeo-request "getOverallAverageMorningFeelScore"))

(defn dates-with-sleep-data []
  (zeo-request "getDatesWithSleepData"))


(defn sleep-stats [date]
  (zeo-request "getSleepStatsForDate" {:date date}))

(defn prev-sleep-stats [date]
  (zeo-request "getPreviousSleepStats" {:date date}))

(defn next-sleep-stats [date]
  (zeo-request "getNextSleepStats" {:date date}))


(defn sleep-record [date]
  (zeo-request "getSleepRecordForDate" {:date date}))

(defn prev-sleep-record [date]
  (zeo-request "getPreviousSleepRecord" {:date date}))

(defn next-sleep-record [date]
  (zeo-request "getPreviousSleepRecord" {:date date}))
