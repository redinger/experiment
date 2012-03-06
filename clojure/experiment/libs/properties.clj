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

(defn load-site-properties []
  (alter-var-root #'site-properties #(load-properties %2) "site.properties"))

(defn- as-keyword [key]
  (cond (string? key) (keyword key)
        (keyword? key) key
        true (throw (java.lang.Error. "Keyword type not recognized"))))

(defn get [property]
  (clojure.core/get site-properties (as-keyword property)))