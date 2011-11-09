(ns experiment.server
  (:use [experiment.infra.middleware])
  (:require [noir.server :as server]
	    [experiment.infra.session :as session]
	    [clojure.data.json :as json]
	    [somnium.congomongo :as mongo]
	    [experiment.infra.api]))

(defonce agent-redirect-rules
  (atom
   {#"/app(.*)"
    [[:iphone "/iphone/app%s"]
     [:ie6 "/not-supported"]
     [:ie7 "/not-supported"]]}))

(server/add-middleware extract-json-payload)
(server/add-middleware session-user)
(server/add-middleware redirect-url-for-user-agent
		       agent-redirect-rules)
(server/load-views "clojure/experiment/views/")

(defonce ^:dynamic *server* (atom nil))

(def mongo-options
  (mongo/mongo-options
   :auto-connect-retry true
   :socketKeepAlive true
   :w 1))

(defn start [& m]
  (mongo/set-connection!
   (mongo/make-connection
    :test {} mongo-options))
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))
	server (server/start
		port {:mode mode
		      :ns 'experiment
		      :session-store (session/mongo-session-store)})]
    (swap! *server* (fn [old] server))))

(defn stop []
  (server/stop @*server*)
  (swap! *server* (fn [a] nil)))

(defn -main []
  (start :prod))


