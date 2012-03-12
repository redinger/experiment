(ns experiment.libs.properties
  (:refer-clojure :exclude [get])
  (:require clojure.java.io))

(defn read-properties [filename]
  (with-open [^java.io.Reader reader (clojure.java.io/reader filename)]
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} (for [[k v] props] [(keyword k) (read-string v)])))))

;;
;; Site properties
;;

(defonce site-properties {})

(defn load-site-properties [& [filename]]
  (alter-var-root #'site-properties #(read-properties %2)
                  (or filename "site.properties")))

(defn- as-keyword [key]
  (cond (string? key) (keyword key)
        (keyword? key) key
        true (throw (java.lang.Error. "Keyword type not recognized"))))

(defn get [property]
  (when (empty? site-properties)
    (load-site-properties))
  (clojure.core/get site-properties (as-keyword property)))

(defn put [property value]
  (alter-var-root #'site-properties assoc property value))