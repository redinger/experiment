(ns experiment.views.home
  (:require
   [experiment.views.common :as common]
   [experiment.infra.session :as session]
   [clojure.data.json :as json]
   [somnium.congomongo :as mongo]
   [noir.response :as resp]
   [noir.util.crypt :as crypt])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
	hiccup.form-helpers))

(def detail-records
  [{:header "Find Treatments"
    :body "This site contains dozens of small lifestyle changes and
     alternative therapies you can try out on your own to see if they help."
    :link "/article/experiments"}
   {:header "Experiment"
    :body "The site can help you run your trial by tracking your treatments and symptoms"
    :link "/article/trials"}
   {:header "Design your own"
    :body "If you have a treatment that you can't find here, you can
	  describe it and design your own experiment"
    :link "/article/design"}
   {:header "Share Outcomes"
    :body "You can journal privately or publically about your experiences.  You can comment and discuss any of the treatments with other people and integrate your experiments with social media"
    :link "/article/sharing"}
   {:header "Simplify Tracking"
    :body "We integrate with the Zeo, your Garmin, Strava.com, your phone's SMS, and e-mail to simplify your self discovery process."
    :link "/article/experiments"}
   {:header "Learn more..."
   :body "This project is an experiment to understand how ordinary
   people can collaborate online to learn more about treatments.  You can learn more by <a href=\"/study1\">joining our study...</a>"
   :link "/article/about"}])
	 
(defpartial render-home-detail [record]
  [:div.home-detail-wrap
    [:div.home-detail
     [:a {:class "home-detail-link" :href (:link record)}
      [:h2 (:header record)]]
     [:p (:body record)]]])

(defpartial render-home-details []
  [:div.home-details
   (map render-home-detail (take 3 detail-records))]
  [:div {:style "clear:both;"}]
  [:hr]
  [:div.home-details
   (map render-home-detail (drop 3 detail-records))])

(defpage "/" {}
;;   (if (session/logged-in?)
;;     (resp/redirect "/app")
   (common/simple-layout {}
    [:div.home-about
     [:h2 "Experiment and share novel therapies for healthy living"]]
;;	(link-to "/article/about" "Read more...")]
    (render-home-details)))
