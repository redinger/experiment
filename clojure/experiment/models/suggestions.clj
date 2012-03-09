(ns experiment.models.suggestions
  (:require [somnium.congomongo :as mongo]
	    [noir.response :as response]
	    [noir.request :as request]
	    [clojure.data.json :as json]
	    [clojure.string :as str])
  (:import [org.bson.types ObjectId])
  (:use noir.core
	experiment.infra.models 
	[experiment.models.core]
	[experiment.models.user]))

;;
;; This file generates a dictionary of autoSuggest objects which
;; are used to generate a list of conditions for searching the
;; system's objects.  Right now we're doing this client side, but
;; we can easily move this to the server later and use mongo
;; to do the search rather than programmatically on the client.
;;

;; Types
(defn- gen-types []
  [{:trigger "show"
    :value "show experiment"
    :title "Experiments"}
   {:trigger "show"
    :value "show treatment"
    :title "Treatments"}
   {:trigger "show"
    :value "show instrument"
    :title "Instruments"}
   {:trigger "show"
    :value "show all"
    :title "All"}])

(defn- symptom-suggestion [tag]
  {:trigger "for"
   :value (str "for " tag)
   :title tag})

;; Symptoms and Conditions
(defn- gen-symptoms+conditions
  "All tags from treatments"
  []
  (map symptom-suggestion
   (reduce clojure.set/union
	   (map #(set (concat (:tags %) (:nicknames %)))
		(fetch-models :treatment :only [:tags :nicknames])))))

;; Instrument
(defn- instrument-suggestion [inst]
  {:trigger "use"
   :title (:variable inst)
   :value (str "use " (serialize-id (:_id inst)))
   :search (str (:nicknames inst) " " (:src inst))
   })

(defn- gen-instruments []
  (map instrument-suggestion
       (fetch-models :instrument :only [:variable :nicknames :src])))

;; Treatment
(defn- treatment-suggestion [treat]
  {:trigger "with"
   :value (str "with " (serialize-id (:_id treat)))
   :title (:name treat)
   :search (:description treat)})
  
(defn- gen-treatments []
  (map treatment-suggestion
       (fetch-models :treatment :only [:name :description])))

(defn compute-suggestions []
  (concat
   (gen-types)
   (gen-treatments)
   (gen-symptoms+conditions)
   (gen-instruments)))


;;
;; API for taking filter conditions and returning a list of model references
;;

(defn- model->client-ref [model]
  (if (and (:type model) (:_id model))
    [(:type model) (serialize-id (:_id model))]
    []))

(defn- model->db-id [model]
  (assert (= (type (:_id model)) ObjectId))
  (:_id model))

(defn- id->db-id [id]
  (mongo/object-id id))

(defn- strip-filters [type filters]
  (map #(.substring % (+ 1 (count type)))
       (filter #(re-find (re-pattern (str "^" type " ")) %) filters)))

(defn- show? [type filters]
  (let [types (set (strip-filters "show" filters))]
    (or (empty? types)
	(types "all")
	(types type))))

(defn- fulltext-regex [filters]
  (str/join "|"
   (filter #(not (re-find #"^(show|with|for|use)" %)) filters)))

(defn- treatment-filter [filters]
  (let [fors (strip-filters "for" filters)
	ids (strip-filters "with" filters)
	fulltext (fulltext-regex filters)]
    (merge (when (not (empty? fors))
	     {:tags {:$in fors}})
	   (when (not (empty? ids))
	     {:_id {:$in ids}})
	   (when (> (count fulltext) 0)
	     {:description {:$regex fulltext :$options "i"}}))))

(defn- filter-treatments [filters]
  (if (some #(re-find #"^use" %) filters)
    []
    (fetch-models :treatment (treatment-filter filters))))

(defn- instrument-filter [filters]
  (let [fulltext (fulltext-regex filters)
	fors (strip-filters "for" filters)]
    (merge
     (when (> (count fulltext) 0)
       {:description {:$regex fulltext :$options "i"}})
     (when (not (empty? fors))
       {:tags {:$all fors}}))))

(defn- filter-instruments [filters]
  (if (some #(re-find #"^use|^with" %) filters)
    []
    (fetch-models :instrument  (instrument-filter filters))))

(defn- experiment-filter [treatments filters]
  (let [fulltext (fulltext-regex filters)
	with-refs (map id->db-id (strip-filters "with" filters))
	treatment-refs (map model->db-id treatments)
	treatment-ids (concat with-refs treatment-refs)
	instrument-ids (map id->db-id (strip-filters "use" filters))]
    (merge (when (not (empty? fulltext))
	     {}) ;; NOTE: fulltext search of comments?
	   (when (not (empty? treatment-ids))
	     {:treatment.$id {:$in treatment-ids}})
	   (when (not (empty? instrument-ids))
	     {:instruments.$id {:$in instrument-ids}}))))

(defn- filter-experiments [treatments filters]
  (fetch-models :experiment (experiment-filter treatments filters)))

(defn filter-models [filters]
  (let [treatments (filter-treatments filters)
	instruments (filter-instruments filters)
	experiments (filter-experiments treatments filters)]
    (concat (when (show? "experiment" filters) experiments)
	    (when (show? "treatment" filters) treatments)
	    (when (show? "instrument" filters) instruments))))
  

(defpage filtered-search "/api/fsearch" {:keys [query limit]}
  (let [filters (str/split query #",")]
    (println filters)
    (response/json
     (vec
      (map model->client-ref
	   (filter-models filters))))))
