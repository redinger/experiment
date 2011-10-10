(ns experiment.server
  (:require [noir.server :as server]
	    [experiment.infra.session :as session]
	    [clojure.data.json :as json]
	    [somnium.congomongo :as mongo]
	    [experiment.infra.api]))

(defn extract-json-payload [handler]
  (fn [req]
    (handler
     (if-let [ctype (get-in req [:headers "content-type"])]
       (do (println (:body req))
	   (if (and (string? ctype) (re-find #"application/json" ctype))
	     (update-in req [:params] assoc :json-payload
			(json/read-json (slurp (:body req)) true false nil))
	     req))
       req))))

(server/add-middleware extract-json-payload)
(server/load-views "clojure/experiment/views/")

(defonce ^:dynamic *server* (atom nil))

(defn start [& m]
  (mongo/mongo! :db :test)
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

(defn -main [& m]
  (start (first m)))


