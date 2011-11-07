(ns experiment.libs.sparkline
  (:require [clojure.data.json :as json]))
  
(def ^:private ids (atom 1))
(defn- sparkline-uid []
  (str "spark-" (swap! ids (partial + 1))))

(defn render-sparkline [data & [params]]
  (let [uid (sparkline-uid)]
    [:span [:span {:id uid}]
     [:script
      (format "$.sparkline(%s, %s, %s)"
	      (str "#" uid)
	      (json/json-str (vec data))
	      (json/json-str (or params {})))]]))