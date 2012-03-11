(defproject experiment "1.0.0-SNAPSHOT"
            :description "Evaluation platform of a new model for running, recording and sharing self-experiments"
	    :url "http://personalexperiments.org"
	    :source-path "clojure"
            :dependencies [[org.clojure/clojure "1.3.0"]
			   [org.clojure/data.json "0.1.1"]
			   [org.clojure/java.jdbc "0.1.1"]
                           [org.clojure/math.numeric-tower "0.0.1"]
			   [org.clojure/tools.logging "0.2.3"]
			   [clj-logging-config "1.9.1"]
			   ;; Framework
                           [noir "1.2.2"]
			   ;; Backends
			   [congomongo "0.1.8"]
			   ;; Clojure Libs
                           [swank-clojure "1.4.0"]
                           [com.draines/postal "1.7-SNAPSHOT"]
			   [clj-http "0.3.0"]
                           [clj-oauth "1.3.1-SNAPSHOT"]
			   [org.clojars.kovasb/clj-json "0.3.2-SNAPSHOT"]
			   [handlebars-clj "0.9-SNAPSHOT"]
			   [clj-time "0.3.2"]
			   [quartz-clj "1.1.0-SNAPSHOT"]
                           [clodown "1.0.2"]
			   ;; Java libs
			   [log4j "1.2.16"]
			   [org.slf4j/slf4j-log4j12 "1.6.1"]]
	    :main experiment.server
	    :daemon {:server {:ns experiment.server
			      :pidfile "./server.pid"}}
            ;; Development Libs
	    :dev-dependencies [[lein-daemon "0.4.0"]
                               [swank-clojure "1.4.0"]
                               [clojure-source "1.3.0"]
                               [lein-marginalia "0.7.0"]
	    		       [lein-multi "1.1.0-SNAPSHOT"]]
            ;; For CDT
            :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
            :extra-classpath-dirs ["/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Classes/classes.jar"]
            )

