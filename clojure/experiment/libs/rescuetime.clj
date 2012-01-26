(ns experiment.libs.rescuetime
  (:require [clj-http.client :as http]
	    [clojure.data.json :as json]))

(def ^:dynamic *api-key* "B63Je4umeLnCdqF974NyTpILpTpQLOMBs7mHC3G3")
(def api-base "https://www.rescuetime.com/anapi/data")

(def dev-key "B63jkG0K78w4K_Mepj8_e_YFaq9kGrBoPp9r1APJ")

(defn api-request [params]
  (json/read-json
   (:body
    (http/get api-base
	      {:query-params
	       (merge
		params
		{:key *api-key*
		 :format "json"
		 :ru "eslick@media.mit.edu"})
	       :content-type :json
	       :accept :json}))))

(defmacro with-key [value & body]
  `(binding [*api-key* (or ~value *api-key)]
     ~@body))

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
  (index-at #(= % column) (results :row_headers)))

(defn- seconds-to-hours [column row]
  (assoc row column (float (/ (nth row column) 3600))))

(defn results-as-hours [results]
  (let [pos (column-index results "Time Spent (seconds)")
	results (update-in results [:row_headers] #(assoc % pos "Time Spent (hours)"))]
    (update-in results [:rows]
	       #(map (partial seconds-to-hours pos) %))))

    

(defn- matching-row-p [column pattern row]
  (re-find pattern (nth row column)))

(defn select-rows-by-name [name type results]
  (let [pos (column-index results type)
	pattern (re-pattern name)]
    (update-in results [:rows]
	       #(filter (partial matching-row-p pos pattern) %))))

(defn efficiency
  ([interval start end]
     (assert (#{"week" "day" "month"} interval))
     (assert (and (string? start) (string? end)))
     (api-request
      {:restrict_kind "efficiency"
       :restrict_begin start
       :restrict_end end
       :perspective "interval"
       :resolution_time interval})))

(defn productivity
  ([interval start end]
     (assert (#{"week" "day" "month"} interval))
     (assert (and (string? start) (string? end)))
     (api-request
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
     (api-request
      {:restrict_kind "category"
       :restrict_begin start
       :restrict_end end
       :perspective "interval"
       :resolution_time interval}))
  ([interval start end name]
     (select-rows-by-name name "Category" (categories interval start end))))
  
(defn activities
  ([interval start end]
     (assert (#{"week" "day" "month"} interval))
     (assert (and (string? start) (string? end)))
     (api-request
      {:restrict_kind "activity"
       :restrict_begin start
       :restrict_end end
       :perspective "interval"
       :resolution_time interval}))
  ([interval start end name]
     (select-rows-by-name name (activities interval start end))))


