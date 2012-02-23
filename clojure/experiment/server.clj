(ns experiment.server
  (:use [experiment.infra.middleware]
	[clj-logging-config.log4j])
  (:require [noir.server :as server]
	    [clojure.tools.logging :as log]
	    [experiment.controller :as ctrl]
	    [experiment.infra.session :as session]
            [experiment.libs.sms :as sms]
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

(defn start [& [mode]]
  ;; Mongo Setup
  (mongo/set-connection!
   (mongo/make-connection
    :test {} mongo-options))
  ;; Setup logging
  (let [mode (keyword (or mode :dev))]
    (if (= mode :dev)
      (set-logger! "default"
		   :level :debug
		   :pattern "%d - %m%n")
      (set-logger! "default"
		   :level :warn
		   :pattern "%d - %m%n"
                   :out "experiment.log"))
    ;; Setup SMS subsystem
    (sms/set-credentials {:user "ianeslick" :pw "az5ure"})
    ;; Start and save server
    (let [port (Integer. (get (System/getenv) "PORT" "8080"))
	  server (server/start
		  port {:mode mode
			:ns 'experiment
			:session-store (session/mongo-session-store)})]
      (swap! *server* (fn [old] server)))))

(defn stop []
  (server/stop @*server*)
  (swap! *server* (fn [a] nil)))

(defn -main []
  (start :prod))


