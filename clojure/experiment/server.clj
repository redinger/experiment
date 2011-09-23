(ns experiment.server
  (:require [noir.server :as server]
	    [clojure.data.json :as json]))

(defn extract-json-payload [handler]
  (fn [req]
    (handler
     (if-let [ctype (get-in req [:headers "content-type"])]
       (if (and (string? ctype) (re-find #"application/json" ctype))
	 (update-in req [:params] assoc :json-payload
		    (json/read-json (slurp (:body req)) true false nil))
	 req)
       req))))

(server/add-middleware extract-json-payload)
(server/load-views "src/experiment/views/")

(defn start [m]
  (somnium.congomongo/mongo! :db :test)
  (let [mode (keyword (or m :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (server/start port {:mode mode
                        :ns 'experiment})))

(defn -main [& m]
  (start (first m)))


