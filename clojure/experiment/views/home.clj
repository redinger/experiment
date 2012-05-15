(ns experiment.views.home
  (:require
   [experiment.views.common :as common]
   [experiment.infra.session :as session]
   [somnium.congomongo :as mongo]
   [noir.response :as resp]
   [noir.util.crypt :as crypt])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        [hiccup.form-helpers :exclude [input form-to]]
        experiment.views.bootstrap))

;; Home Page Layout
;; ----------------------------

;;
;; ## Carousel
;;

(defn academy-health []
  [:div#academyHealth.hero-unit
   [:div.pull-right
    [:img {:style "height: 200px; margin-top: -40px; margin-right: 40px;"
           :src "img/academy-health.jpg"}]]
   [:h1 "In the News"]
   [:p "PersonalExperiments.org wins " [:a {:href "http://blog.academyhealth.org/?p=574"} "runner-up"] " in " [:a {:href "http://academyhealth.org"} "Academy Health's"] [:br] [:a {:href "http://www.health2challenge.org/relevant-evidence-to-advance-care-and-health-reach/"} "REACH competition"] " under the entry 'Aggregated Self-Experiments'"]
   [:small [:em "January 31st, 2012"]]])

(defn study1-announce []
  [:div#study1.hero-unit
   [:h1 "Author an Experiment"]
   [:p "PersonalExperiments.org is dedicated to help individuals discover effective ways to improve their health and wellbeing.  Join our first MIT-run " [:a {:href "/study1"} "authoring study"] " today to help us create better tools for you."]
   [:small [:em "March 15th, 2012"]]])

(defn home-carousel [id]
  (carousel id (list (study1-announce)
                     (academy-health))))
         


;; ## Feature Boxes

(def detail-records
  [{:header "Find Treatments"
    :body "This site contains dozens of small lifestyle changes and
     alternative therapies you can try out on your own to see if they help."
    :link "/article/treatment"}
;;    :icon "icon-plus"}
   {:header "Experiment"
    :body "The site can help you try out new treatments by running
    structured, personal experiments that help you track your
    treatments, symptoms, and assess impact."
    :link "/article/experiment"}
;;    :icon "icon-random"}
   {:header "Simplify Tracking"
    :body "We integrate with the Zeo, Strava.com, your phone's SMS,
    and over e-mail to simplify your self discovery process.  More
    integrations are coming!"
    :link "/article/track"}
   {:header "Share Outcomes"
    :body "You can journal privately or publicly about your
    experiences.  You can comment and discuss treatments or
    experiments with other users or on social media"
    :link "/article/share"}
   {:header "Design your own"
    :body "You can use a previously-defined experiment, or you can add
    your own treatments and experiments. You can help improve this
    feature by <a href=\"/study1\">participating in our study.</a>"
    :link "/article/design"}
   {:header "Learn more..."
   :body "This project is a work in progress to help us understand how
   ordinary people can collaborate to make personal discoveries about
   treatments." ;; See our <a href=\"/article/about\">demo</a>"
   :link "/article/about"}])
	 
(defpartial render-home-detail [record]
  [:li.span4.home-detail
   [:div.thumbnail
    [:h2 [:a {:href (:link record)} [:i {:class (:icon record)}] " " (:header record)]]
    [:p (:body record)]]])

(defpartial home-page-details []
  [:ul.thumbnails
   (map render-home-detail detail-records)])


;; ### Sponsor Bar

(defn render-thumbnail [url imgurl & {:as styles}]
  [:a.thumbnail {:href url }
   [:img (assoc styles :src imgurl)]])

(defpartial sponsor-bar []
  [:div.sponsor-bar
   [:div.byline
    "Created/Sponsored By"]
   [:div
    [:ul.thumbnails
     [:li {:style "padding-top:25px;"}
      (render-thumbnail "http://lybba.org"
                        "http://www.lybba.org/wp-content/uploads/images/lybba_logo.png"
                        :alt "Lybba.org")]
     [:li
      (render-thumbnail "http://www.media.mit.edu/"
                        "http://t3.gstatic.com/images?q=tbn:ANd9GcQ9s6ovS4qdVf568bDL9Xdn8xAKTgaJIhdbdi1-jPq9lc-qYjxGYw"
                        :alt "MIT Media Laboratory"
                        :width "130px")]
     [:li
      (render-thumbnail "http://c3nproject.org/"
                        "http://c3nproject.org/sites/bmidrupalpc3n.chmcres.cchmc.org/files/c3ntheme_logo.png"
                        :alt "C3N Project")]]]])


;;
;; ## Home Page
;;


(defpage public-home-page "/" {}
  (common/layout
   ["Welcome to Personal Experiments"
    (common/default-nav "Home")
    :deps ["views/home" "libs/misc/jstz.min"]]
   [:div.container.home-page
    (home-carousel "homeCarousel")
    (home-page-details)
    (sponsor-bar)]))

