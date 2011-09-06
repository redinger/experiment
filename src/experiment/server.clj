(ns experiment.server
  (:require [noir.server :as server]))

(server/load-views "src/experiment/views/")

(defn start [m]
  (let [mode (keyword (or m :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (server/start port {:mode mode
                        :ns 'experiment})))

(defn -main [& m]
  (start (first m)))


