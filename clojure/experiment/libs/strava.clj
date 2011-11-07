(ns experiment.libs.strava
  (:use experiment.infra.models)
  (:require [clj-http.client :as http]
	    [clj-time.core :as time]
	    clj-time.format
	    [clojure.data.json :as json]
	    [somnium.congomongo :as mongo]))


(def ^:dynamic *strava-base* "http://www.strava.com/api/v1/%s")

(defn strava-url [command]
  (format *strava-base* command))

(defn strava-request [command & args]
  (let [argmap (apply hash-map args)]
    (json/read-json
     (:body (http/get (strava-url command)
		      {:query-params argmap}))
     true)))

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

