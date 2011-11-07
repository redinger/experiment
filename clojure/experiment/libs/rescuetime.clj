(ns experiment.libs.rescuetime
  (:require [clj-http.client :as http]
	    [clojure.data.json :as json]))

(def *api-key* "B63Je4umeLnCdqF974NyTpILpTpQLOMBs7mHC3G3")
(def *api-base* "https://www.rescuetime.com/anapi/data")

(defn request [params]
  (json/read-json
   (:body
    (http/get *api-base*
	      {:query-params
	       (merge
		params
		{:key *api-key*
		 :format "json"})
	       :content-type :json
	       :accept :json}))))

(defn index-at
  "Return the index, appropriate for assoc and nth, of a vector
   where the function provided returns true"
  ([pred sequence]
     (loop [i 0
	    s sequence]
       (cond (empty? s) nil
	     (pred (first s)) i
	     true (recur (inc i) (rest s))))))

(defn column-index [results column]
  (index-at #(= % column) (results "row_headers")))

(defn results-as-hours [results]
  (let [pos (column-index results "Time Spent (seconds)")]
    (update-in results ["rows"]
	       (fn [rows] (map #(assoc % pos (float (/ (nth % pos) 3600))) rows)))))

(defn select-rows-by-name [name type results]
  (let [pos (column-index results type)]
    (update-in results ["rows"]
	       (fn [rows] (filter #(re-find (re-pattern name) (nth % pos)) rows)))))

(defn efficiency
  ([interval start end]
     (assert (#{"week" "day" "month"} interval))
     (assert (and (string? start) (string? end)))
     (request
      {:restrict_kind "efficiency"
       :restrict_begin start
       :restrict_end end
       :perspective "interval"
       :resolution_time interval})))

(defn productivity
  ([interval start end]
     (assert (#{"week" "day" "month"} interval))
     (assert (and (string? start) (string? end)))
     (request
      {:restrict_kind "productivity"
       :restrict_begin start
       :restrict_end end
       :perspective "interval"
       :resolution_time interval})))
;;  ([interval start end name]
     

(defn categories
  ([interval start end]
     (assert (#{"week" "day" "month"} interval))
     (assert (and (string? start) (string? end)))
     (request
      {:restrict_kind "category"
       :restrict_begin start
       :restrict_end end
       :perspective "interval"
       :resolution_time interval}))
  ([interval start end name]
     (select-rows-by-name name (categories interval start end))))
  
(defn activities
  ([interval start end]
     (assert (#{"week" "day" "month"} interval))
     (assert (and (string? start) (string? end)))
     (request
      {:restrict_kind "activity"
       :restrict_begin start
       :restrict_end end
       :perspective "interval"
       :resolution_time interval}))
  ([interval start end name]
     (select-rows-by-name name (activities interval start end))))