(ns experiment.views.discover
  (:use noir.core)
  (:require [experiment.infra.models]
	    [experiment.infra.api]
	    [experiment.views.app]
	    [noir.response :as response]
	    [clojure.tools.logging :as log]))

(defpage discover-suggest [:get "/api/suggest/discover/"] {:keys [q limit]}
  (println q)
  (response/json
   [{:id "1" :name "sleep"}
    {:id "2" :name "exercise"}
    {:id "3" :name "foobaring"}
    {:id "4" :name "resting" :description "could be sleep"}]))
  