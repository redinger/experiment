(ns experiment.libs.rescuetime
  (:require [clj-http.client :as http]
            [experiment.libs.datetime :as dt]
	    [clojure.data.json :as json]))

(def ^:dynamic *api-key* nil)
(def api-base "https://www.rescuetime.com/anapi/data")

(defn api-request [params]
  (json/read-json
   (:body
    (http/get api-base
	      {:query-params
	       (merge
		params
		{:key *api-key*
		 :format "json"})
               :content-type :json
               :accept :json}))))

(defmacro with-key [value & body]
  `(binding [*api-key* (or ~value (props/get :rescuetime.api-key))]
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

(defn filter-rows-by-name
  ([name type results]
     (let [pos (column-index results type)
           pattern (re-pattern name)]
       (update-in results [:rows]
                  #(filter (partial matching-row-p pos pattern) %)))))

(defonce ^:dynamic *test* nil)

(defn aggregate-rows
  [group]
  [(first group)
   (apply + (map second (second group)))])

(defn aggregate-results
  ([results]
     (alter-var-root #'*test* (fn [a] (:rows results)))
     (assoc results
       :row_headers (take 2 (:row_headers results))
       :rows (map aggregate-rows
                  (group-by first (:rows results))))))
   
(defn efficiency
  ([interval start end]
     (assert (#{"week" "day" "month"} interval))
     (api-request
      {:restrict_kind "efficiency"
       :restrict_begin (dt/as-iso-8601-date start)
       :restrict_end (dt/as-iso-8601-date end)
       :perspective "interval"
       :resolution_time interval})))

;;(defn- parse-productivity [result]
;;  (assoc result :rows
;;         (map (fn [[ts score 

(defn productivity
  ([interval start end]
     (assert (#{"week" "day" "month"} interval))
;;     (parse-productivity
      (api-request
       {:restrict_kind "productivity"
        :restrict_begin (dt/as-iso-8601-date start)
        :restrict_end (dt/as-iso-8601-date end)
        :perspective "interval"
        :resolution_time interval})))

(defn categories
  ([interval start end]
     (assert (#{"week" "day" "month"} interval))
     (api-request
      {:restrict_kind "category"
       :restrict_begin (dt/as-iso-8601-date start)
       :restrict_end (dt/as-iso-8601-date end)
       :perspective "interval"
       :resolution_time interval}))
  ([interval start end name]
     (filter-rows-by-name name "Category" (categories interval start end))))
  
(defn activities
  ([interval start end]
     (assert (#{"week" "day" "month"} interval))
     (api-request
      {:restrict_kind "activity"
       :restrict_begin (dt/as-iso-8601-date start)
       :restrict_end (dt/as-iso-8601-date end)
       :perspective "interval"
       :resolution_time interval}))
  ([interval start end name]
     (assert (#{"week" "day" "month"} interval))
     (api-request
      {:restrict_kind "activity"
       :restrict_begin (dt/as-iso-8601-date start)
       :restrict_end (dt/as-iso-8601-date end)
       :perspective "interval"
       :restrict_thing name
       :resolution_time interval})))

;; Specific interfaces

(defn facebook
  ([interval start end]
     (aggregate-results
      (activities interval start end "facebook.com"))))

(defn social-media
  ([interval start end]
     (categories interval start end "General Social Networking")))
      