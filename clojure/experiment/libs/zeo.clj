(ns experiment.libs.zeo
  (:use experiment.infra.models)
  (:require [clj-http.client :as http]
	    [clojure.data.json :as json]))

(def ^:dynamic *api-key* "ACE41D854610E84DAF16419E087C2ADF")
(def ^:dynamic *api-base* "https://api.myzeo.com:8443/zeows/api/v1/json/sleeperService/%s")


(def ^:dynamic *staging-key* "6B58F54966A8A9632A68EBBFF0192D4C")
(def ^:dynamic *staging-base* "https://staging.myzeo.com:8443/zeows/api/v1/json/sleeperService/%s")
  
(defn zeo-url [action]
  (format *staging-base* action))

(defn zeo-request [auth action & [params]]
;;  (json/parse-string
;   (:body
  (http/get (zeo-url action)
	    {:query-params (assoc params :key *api-key*)
	     :basic-auth auth
	     :content-type :json
	     :accept :json}));;))
