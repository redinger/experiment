(ns experiment.server
  (:use
   [experiment.infra.middleware]
   [clj-logging-config.log4j])
  (:require
   [noir.server :as server]
   [clojure.tools.logging :as log]
   [experiment.controller :as ctrl]
   [experiment.infra.session :as session]
   [experiment.libs.sms :as sms]
   [experiment.libs.properties :as props]
   [experiment.libs.fulltext :as ft]
   [experiment.models.events :as events]
   [experiment.models.trackers :as track]
   [somnium.congomongo :as mongo]
   [experiment.infra.api]))


;;
;; Define redirection rules for different client types
;;

(defonce agent-redirect-rules
  (atom
   {#"/app(.*)"
    [;;[:iphone "/iphone/app%s"]
     [:ie6 "/not-supported"]
     [:ie7 "/not-supported"]]}))

(server/add-middleware redirect-url-for-user-agent
		       agent-redirect-rules)

;; Make body available as parsed JSON when mime type is json
(server/add-middleware extract-json-payload)

;; Always track the current user when logged in
(server/add-middleware session-user)

;; Load all the site views
(server/load-views "clojure/experiment/views/")

;; Ensure MongoDB users keep alive and retry
(def mongo-options
  (mongo/mongo-options
   :auto-connect-retry true
   :socketKeepAlive true
   :w 1))

;; Store the Noir/Jetty server instance
(defonce noir nil)

;; -----------------------
;; Start Experiment Server
;; -----------------------
;; 
;; Sets up various subsystems, default database connection,
;; starts Noir server and Quartz scheduler.
;; 
;; Pass in :dev or :prod modes (or pulls from site.properties file)
;; Be sure to setup the site.properties file before running the site
;; FYI - site.properties is read at compile time for many subsystems
;; such as various services, see experiment.libs.properties
;;

(defn start [& [mode]]
  ;; Update mode property for site conditionals
  (when mode (props/put :mode mode))
  
  ;; Mongo Setup
  (mongo/set-connection!
   (mongo/make-connection
    (props/get :db.name) {} mongo-options))

  ;; Indexing setup
  (ft/start)
  
  ;; Setup logging
  (let [mode (keyword (or mode (props/get :mode) :dev))]
    (if (= mode :dev)
      (do (server/add-middleware swank-connection)
          (set-loggers! "default"
                        {:level :warn
                         :pattern "%d - %m%n"}
                        "experiment"
                        {:level :debug
                         :pattern "%d - %m%n"}
                        "org.mortbay.log"
                        {:level :error}
                        "org.quartz.core.QuartzSchedulerThread"
                        {:level :error}))
      (set-logger! "default"
		   :level :warn
		   :pattern "%d - %m%n"
                   :out "experiment.log"))

    ;; Setup SMS subsystem
    (sms/set-credentials {:user (props/get :sms.username)
                          :pw (props/get :sms.password)})
    (sms/set-reply-handler 'track/sms-reply-handler)

    ;; Start and save server
    (let [port (Integer. (get (System/getenv) "PORT" "8080"))
	  server (server/start
		  port {:mode (props/get :mode)
			:ns 'experiment
			:session-store (session/mongo-session-store)})]
      (alter-var-root #'noir (fn [old] server)))

    ;; Start event scheduler
    (ctrl/start)))


(defn stop []
  (ctrl/stop)
  (server/stop noir)
  (alter-var-root #'noir (fn [a] nil)))

(defn -main
  "The default project entry points starts the server in production mode"
  []
  (start :prod))
