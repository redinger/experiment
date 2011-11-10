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
  [{:header "Find Personal Experiments"
    :body "This site contains dozens of small lifestyle changes and
     alternative therapies you can try out on your own to see if they help."
    :link "/article/experiments"}
   {:header "Run a Self-Trial"
    :body "The site can help you run your trial by tracking your treatments and symptoms"
    :link "/article/trials"}
   {:header "Design your own experiments"
    :body "If you have a treatment that you can't find here, you can
	  describe it and design your own experiment"
    :link "/article/design"}
   {:header "Share your experiences"
    :body "You can journal privately or publically about your experiences.  You can comment and discuss any of the treatments with other people and integrate your experiments with social media"
    :link "/article/sharing"}
   {:header "Integrate with your devices"
    :body "We integrate with the Zeo, your Garmin, Strava.com, your phone's SMS, and e-mail to simplify your self discovery process."
    :link "/article/experiments"}
   {:header "Learn about this service"
   :body "This project is an experiment to understand better how
   ordinary people online can collaborate to learn more about
   treatments"
   :link "/article/about"}])
	 
(defpartial render-home-detail [record]
  [:div.home-detail-wrap
   [:a {:class "home-detail-link" :href (:link record)}
    [:div.home-detail
     [:h2 (:header record)]
     [:p (:body record)]]]])

(defpartial render-home-details []
  [:div#home-details
   (map render-home-detail detail-records)])
  

 (defpage "/" {}
;;   (if (session/logged-in?)
;;     (resp/redirect "/app")
     (common/simple-layout
       [:div#home-about
	[:p "Welcome to the Invent Health site for personal experimentation.  This site is intended to help you share your experiences with lifestyle interventions, alternative therapies, with each other and with your medical professional.  We seek to help you better understand if and how much changes you are making to your life are having the impact you want. &nbsp;"
	 (link-to "/article/about" "Read more...")]]
       (render-home-details)
       [:div#home-footer
	(link-to "/article/terms" "Terms of Use") "|"
	(link-to "/article/privacy" "Privacy") "|"
	(link-to "/article/about" "About")]))
	


