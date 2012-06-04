(ns experiment.setup
  (:use clojure.test)
  (:require [somnium.congomongo :as mongo]))

(def test-db
  (mongo/make-connection "clj-test"))

(defn database-fixture [f]
  (mongo/with-mongo test-db
    (f)))

(defn clean-database-fixture [f]
  (mongo/drop-database! "clj-test")
  (database-fixture f)
  (mongo/drop-database! "clj-test"))

