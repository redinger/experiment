(ns experiment.views.profile
  (:require
   [experiment.views.common :as common]
   [clojure.data.json :as json]
   [experiment.infra.session :as session])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers))

(defpage render-my-profile-page "/profile" {:as params}
  (session/put! :test "Foo bar")
  (let [visits (session/inc! :visits)]
    (common/simple-layout
     [:h1 "About Me"]
     [:p (session/get :test)])))
     

