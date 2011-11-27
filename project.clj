(defproject experiment "0.9.0-SNAPSHOT"
            :description "Evaluation platform of a new model for running, recording and sharing self-experiments"
	    :url ""
	    :source-path "clojure"
            :dependencies [[org.clojure/clojure "1.3.0"]
			   [org.clojure/data.json "0.1.1"]
			   [org.clojure/tools.logging "0.2.3"]
			   [log4j/log4j "1.2.16"]
                           [noir "1.2.1"]
			   ;; Backend
			   [congomongo "0.1.7-SNAPSHOT"]
			   ;; Utilities for libs
			   [quartz-clj "1.1.0-SNAPSHOT"]
			   [clj-http "0.2.5"]
			   [clj-logging-config "1.9.1"]
			   [org.clojars.kovasb/clj-json "0.3.2-SNAPSHOT"]
			   [clj-time "0.3.2"]]
	    :dev-dependencies [[swank-clojure "1.3.0-SNAPSHOT"]
			       [lein-daemon "0.4.0"]
			       [lein-run "1.0.0-SNAPSHOT"]]
	    :daemon {:server {:ns experiment.server
			      :pidfile "./server.pid"}}
	    :main experiment.server)

