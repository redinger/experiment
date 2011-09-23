(defproject experiment "0.1.0-SNAPSHOT"
            :description "Evaluation platform of a new model for running, recording and sharing self-experiments"
	    :url ""
	    :source-path "clojure"
            :dependencies [[org.clojure/clojure "1.3.0-beta3"]
			   [org.clojure/data.json "0.1.1"]
                           [noir "1.1.1-SNAPSHOT"]
			   ;; Backend
			   [congomongo "0.1.7-SNAPSHOT"]
			   ;; Utilities for libs
			   [woven/clj-http "0.1.2-SNAPSHOT"]
			   [woven/clj-json "0.3.1"]
			   [clj-time "0.2.0-SNAPSHOT"]
			   [clj-glob "1.0.0"]]
	    :dev-dependencies [[swank-clojure "1.3.0-SNAPSHOT"]
			       [lein-daemon "0.2.1"]
			       [lein-run "1.0.0-SNAPSHOT"]]
	    :main experiment.server)

