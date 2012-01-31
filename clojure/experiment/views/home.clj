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
    :body "The site can help you try out new treatments by running
    structured, personal experiments that help you track your
    treatments, symptoms, and assess impact."
    :link "/article/trials"}
   {:header "Design your own"
    :body "You can use a previously-defined experiment, or you can add
    your own treatments and experiments. You can help this feature by <a
    href=\"/study1\">participating in our study.</a>"
    :link "/article/design"}
   {:header "Share Outcomes"
    :body "You can journal privately or publicly about your
    experiences.  You can comment and discuss treatments or
    experiments with other users or on social media"
    :link "/article/sharing"}
   {:header "Simplify Tracking"
    :body "We integrate with the Zeo, Strava.com, your phone's SMS,
    and over e-mail to simplify your self discovery process.  More
    integrations are coming!"
    :link "/article/experiments"}
   {:header "Learn more..."
   :body "This project is a work in progress to help us understand how
   ordinary people can collaborate to make personal discoveries about
   treatments. See our <a href=\"/article/about\">demo</a>"
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

(defpartial render-home-newsflash []
  [:div.home-flash
   [:h3 "This site was submitted to the Academy Health REACH contest last November and we are happy to announce that we were selected as one of two runners-up.  You can register now on the site and you will be notified when the site is open for general use later this month."]])

(defn render-image-link [url imgurl & {:as styles}]
  [:a {:href url}
   [:img (assoc styles :src imgurl)]])
  

(defpartial render-logos []
  [:div.home-logo-header
   [:b "Sponsored By"]]
  [:div.home-logos
   (render-image-link "http://lybba.org"
                      "http://www.lybba.org/wp-content/uploads/images/lybba_logo.png"
                      :alt "Lybba.org")
   (render-image-link "http://www.media.mit.edu/"
                      "http://t3.gstatic.com/images?q=tbn:ANd9GcQ9s6ovS4qdVf568bDL9Xdn8xAKTgaJIhdbdi1-jPq9lc-qYjxGYw"
                      :alt "MIT Media Laboratory"
                      :width "130px")
   (render-image-link "http://c3nproject.org/"
                      "http://c3nproject.org/sites/bmidrupalpc3n.chmcres.cchmc.org/files/c3ntheme_logo.png"
                      :alt "C3N Project")])

(defpage "/" {}
;;   (if (session/logged-in?)
;;     (resp/redirect "/app")
   (common/simple-layout {}
;;    [:div.home-about
                         ;;     [:h2 "Experiment and share novel therapies for healthy living"]]
    [:div.home-about
     [:h2 [:b "Newsflash: "] " PersonalExperiments.org wins " [:a {:href "http://academyhealth.org"} "runner-up"] " in " [:a {:href "http://academyhealth.org"} "Academy Health's"] [:br] [:a {:href "http://www.health2challenge.org/relevant-evidence-to-advance-care-and-health-reach/"} "REACH competition"] " under the entry 'Aggregated Self-Experiments'"]]
;;    (render-home-newsflash)
    (render-home-details)
    [:div {:style "clear: both;"}]
    [:hr]
    (render-logos)))
