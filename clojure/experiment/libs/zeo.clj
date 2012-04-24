(ns experiment.libs.zeo
  (:use experiment.infra.models)
  (:require [clj-http.client :as http]
            [experiment.libs.datetime :as dt]))


;; "ACE41D854610E84DAF16419E087C2ADF" ;; mit.edu
;; "6B58F54966A8A9632A68EBBFF0192D4C" ;; media.mit.edu

(def ^{:dynamic true} *std-key* "ACE41D854610E84DAF16419E087C2ADF")
(def ^{:dynamic true} *std-base* "https://api.myzeo.com:8443/zeows/api/v1/json/sleeperService/%s")

(def ^{:dynamic true} *staging-key* "6B58F54966A8A9632A68EBBFF0192D4C")
(def ^{:dynamic true} *staging-base* "https://staging.myzeo.com:8443/zeows/api/v1/json/sleeperService/%s")
  
(def zeo-mode :standard)
(defonce ^{:dynamic true} *auth* nil)

(defn- zeo-key []
  (if (= zeo-mode :staging)
    *staging-key*
    *std-key*))

(defn- zeo-base []
  (if (= zeo-mode :staging)
    *staging-base*
    *std-base*))

(defn- zeo-url [action]
  (format (zeo-base) action))

(defn- valid-auth? [auth]
  (and (sequential? auth)
       (= (count auth) 2)
       (every? string? auth)))

(defn set-default-auth [auth]
  (assert (valid-auth? auth))
  (alter-var-root #'*auth* (fn [orig] auth)))
  
(defn get-default-auth []
  *auth*)

(defn- with-auth [auth & body]
  `(let [*auth* auth]
     ~@body))

(defn zeo-request
  ([auth action params]
     (assert (valid-auth? auth))
     (assert (map? params))
     (http/get (zeo-url action)
               {:as :json
                :query-params (assoc params :key (zeo-key))
                :basic-auth auth
                :content-type :json
                :accept :json}))
  ([action params]
     (zeo-request *auth* action params))
  ([action]
     (zeo-request *auth* action {})))

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
