(ns experiment.infra.middleware
  (:require
   [somnium.congomongo :as mongo]
   [noir.session :as session]
   [noir.request :as req]
   [noir.response :as resp]
   [clojure.data.json :as json]
   [clojure.string :as str]))


;;
;; # Middleware: Bind the current user
;;

(def ^:dynamic *current-user* nil)

(defn session-user
  "RING MIDDLEWARE: Binds the current session user, when logged in.
   Use tools in infra/sessions and infra/auth to login and logout
   users" [handler]
  (fn [req]
    (let [userid (session/get :userid)]
      (binding [*current-user*
		(and userid
		     (mongo/fetch-one :user :where {:_id userid}))]
	(handler req)))))

;;
;; # Middleware: pre-parse JSON payloads
;;
   
(defn extract-json-payload
  "RING MIDDLEWARE: When the POST content type is application/json,
   parse the data and make available in parameter list as :json-payload"
  [handler]
  (fn [req]
    (handler
     (if-let [ctype (get-in req [:headers "content-type"])]
       (if (and (string? ctype) (re-find #"application/json" ctype))
         (update-in req [:params] assoc :json-payload
                    (json/read-json (slurp (:body req)) true false nil))
         req)
       req))))

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
