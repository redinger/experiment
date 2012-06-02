(ns experiment.infra.middleware
  (:require
   [clojure.tools.logging :as log]
   [somnium.congomongo :as mongo]
;;   [experiment.infra.session :as session2]
   [noir.session :as session]
   [noir.request :as req]
   [noir.response :as resp]
   [cheshire.core :as json]
   [clojure.string :as str]))


;;
;; # Middleware: Support breaking to swank in handlers
;;

(defn swank-connection
  "RING Middleware: Binds *current-connection* to the
   swank connection that was active at the time the server
   was started."
  [handler] 
  (let [conn swank.core.connection/*current-connection*] 
    (fn [request] 
      (binding [swank.core.connection/*current-connection* conn] 
        (handler request)))))

;;
;; # Middleware: Bind the current user
;;

;; # User fetcher: (fn & {:keys [id username]}) => canonical user object
;;   defined by downstream dependencies to set the current user
(def user-fetcher nil)

(defn set-user-fetcher [ffn]
  (assert (fn? ffn))
  (alter-var-root #'user-fetcher (fn [old] ffn)))

(def ^{:dynamic true} *current-user* nil)

(defn session-user
  "RING MIDDLEWARE: Binds the current session user, when logged in.
   Use tools in infra/sessions and infra/auth to login and logout
   users"
  [handler]
  (fn [req]
    (let [userid (session/get :userid)]
      (binding [*current-user*
                (and userid user-fetcher
                     (user-fetcher :id userid))]
        (handler req)))))


;;
;; # Middleware: Session timezone
;;

(def ^{:dynamic true} *timezone* nil)

(defn user-timezone []
  (when *current-user*
    (let [tz (or (get-in *current-user* [:preferences :tz])
                 (get-in *current-user* [:preferences :tz_default]))]
      (when (> (count tz) 0)
        (org.joda.time.DateTimeZone/forID tz)))))

(defn user-timezone-default! [default]
  (when *current-user*
    (println "Setting default for " (:username *current-user*))
    (somnium.congomongo/update! :user
                                {:_id (:_id *current-user*)}
                                {:$set {:preferences.tz_default default}}
                                :upsert false)))

(defn request-timezone [req]
  (let [stz (session/get :timezone)
        rtz (get-in req [:params :_timezone])]
    (when (and (not stz) rtz)
      (session/put! :timezone rtz))
    (org.joda.time.DateTimeZone/forID (or stz rtz nil))))

(defn compute-timezone [req]
  (let [user-tz (user-timezone)
        req-tz (request-timezone req)
        default (org.joda.time.DateTimeZone/getDefault)]
    (when (and (not user-tz) req-tz)
      (user-timezone-default! req-tz))
    (if-let [tz (or user-tz req-tz)]
      (or tz default)
      default)))

(defn session-timezone-handler
  "RING Middleware: Binds *timezone* to a timezone based on the
   following algorithm (assumes session-user is set in chain)
   - User object timezone (Home TZ explicitly set by user)
   - Detected browser timezone (if set on session by javascript)
   - Server timezone (default)"
  [handler]
  (fn [req]
    (let [tz (compute-timezone req)]
      (binding [*timezone* tz]
        (handler req)))))

(defn session-timezone
  "Use active session TZ; default to server's TZ"
  []
  (or *timezone*
      (org.joda.time.DateTimeZone/getDefault)))

(defn server-timezone
  "Server TZ"
  []
  (org.joda.time.DateTimeZone/getDefault))

  

;;
;; # Middleware: pre-parse JSON payloads
;;
   
(defn- extract-json [req]
  (if-let [ctype (get-in req [:headers "content-type"])]
    (if (and (string? ctype) (re-find #"application/json" ctype)
             (not (get-in req [:params :json-payload])))
      (update-in req [:params] assoc :json-payload
                 (try
                   (let [serialized (slurp (:body req))]
;;                     (println "Serialized: " serialized)
                     (json/parse-string serialized true))
                   (catch java.lang.Throwable e
                     (log/error "Ignoring JSON payload"))))
      req)
    req))

(defn extract-json-payload
  "RING MIDDLEWARE: When the content type is application/json,
   parse the data and make available in parameter list as :json-payload"
  [handler]
  (fn [req]
    (handler (extract-json req))))

;;
;; # Middleware: redirect to a URL based on the user agent
;;
;; - Very special purpose facility
;; - Be nice to make more general?

(def user-agent-names
  {:iphone #".*iPhone.*"
   :msie #".*MSIE.*"
   :ie<8 #".*MSIE\s[1-7].*"
   :ie8 #".*MSIE\s8.*"
   :ie9 #".*MSIE\s9.*"
   :safari #".*Safari.*"
   :mozilla #".*Mozilla.*"
   :firefox #".*Firefox.*"
   :droid1 #".*Android\s1.*"
   :droid2 #".*Android\s2.*"})

(defn- multiple-matches-error [matches]
  (throw
   (java.lang.Error.
    (format "Multiple matches: %s" (map first matches)))))

(defn- match-agent-and-rewrite [agent-string matches [agent-tag rewrite]]
  (let [regex (user-agent-names agent-tag)]
    (when (and regex (re-find regex agent-string))
      (apply format rewrite matches))))

(defn- match-agent [agent-string matches agent-rules]
  (some (partial match-agent-and-rewrite agent-string matches) agent-rules))

(defn- match-uri-prefix [uri rule]
  (let [[prefix-regex agents] rule]
    (when-let [matches (re-matches prefix-regex uri)]
      [prefix-regex (rest matches) agents])))
   
(defn- match-prefix [uri rules]
  (let [matches (keep (partial match-uri-prefix uri) rules)]
    (cond (= (count matches) 0) nil
	  (> (count matches) 1) (multiple-matches-error matches)
	  true (rest (first matches)))))
   
(defn redirect-url-for-user-agent
  "RING MIDDLEWARE: When the User Agent matches the provided rules,
   rewrite the URL appropriately and redirect to it.
   [base agent target]"
  [handler prefix-map]
  (fn [req]
    (let [uri (:uri req)
	  user-agent ((:headers req) "user-agent")
	  prefix-rule-map (if (= (type prefix-map) clojure.lang.Atom)
			    (deref prefix-map)
			    prefix-map)]
      (if-let [[matches agent-rules] (match-prefix uri prefix-rule-map)]
	(if-let [new-uri (match-agent user-agent matches agent-rules)]
	  (do (println "Agent redirect of " uri " to " new-uri)
	      (resp/redirect new-uri))
	  (handler req))
	(handler req)))))
