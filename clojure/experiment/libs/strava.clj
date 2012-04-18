(ns experiment.libs.strava
  (:use experiment.infra.models)
  (:require
   [experiment.infra.services :as services]
   [clj-http.client :as http]
   [clj-time.core :as time]
   [clojure.string :as str]
   clj-time.format
   [cheshire.core :as json]
   [somnium.congomongo :as mongo]))

(services/register
 :strava
 ["Strava"
  :description "Strava users have the ability to create an instrument to track their performance on a specific segment over time.  Just enter the segment name here and we'll pull the segment data from all the rides you do."]
 :email {:title "Account Email"}
 :password {:title "Password" :type "Password"}
 :segment1 {:title "Segment Name (1)"}
 :segment2 {:title "Segment Name (2)"})

(def ^:dynamic *strava-base* "http://www.strava.com/api/v1/%s")
(def strava-base "https://www.strava.com/api/v2/%s")

(defn strava-url [command]
  (format strava-base command))

(defn strava-request [command params]
  (let [cmd (if (vector? command)
              (str/join "/" (map name command))
              command)]
    (json/parse-string
     (:body ((if (= (:method params) :post) http/post http/get)
             (strava-url command)
             {:query-params params}))
     true)))

;;
;; Authenticate
;;

(defn authenticate-strava [params]
  (let [result (strava-request [:authentication :login] params)]
    (if (= (:success result) "success")
      (select-keys result [:token :athelete_id])
      nil)))

;; Segments

(defn search-segments [segment]
  (:segments (strava-request "segments" :name segment)));;(clj-http.util/url-encode segment)))

(defmacro cached-mongo-fetch-one [coll where selector cmd & args]
  `(let [cached# (mongo/fetch-one ~coll :where ~where)]
     (if (empty? cached#)
       (let [segment# (~selector (apply strava-request ~cmd ~args))]
	 (mongo/insert! :segments segment#)
	 segment#)
       cached#)))

(defn segment [ref]
  (cond (number? ref)
	(cached-mongo-fetch-one :segments {:id ref} :segment (format "segments/%d" ref))
	(string? ref)
	(mongo/fetch-one :segments :where {:short ref})))

(defn find-segments [segment]
  nil)

(defn segment-efforts [segment user]
  (:efforts
   (strava-request
    (format "segments/%d/efforts"
	   (if (number? segment) segment
	       (:id (first (find-segments segment)))))
    :athleteName user)))

(defn effort [id]
  (cached-mongo-fetch-one :efforts {:id id} :effort (format "efforts/%d" id)))

(defn parse-strava-date [date-string]
  (.getMillis
   (clj-time.format/parse
    (:date-time-no-ms clj-time.format/formatters)
    date-string)))

(defn as-date-time [millis]
  (org.joda.time.DateTime. millis))

