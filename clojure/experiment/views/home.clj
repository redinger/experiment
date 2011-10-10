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

 (defpage "/" {}
;;   (if (session/logged-in?)
;;     (resp/redirect "/app")
     (common/simple-layout
      [:div#home-main
       [:div#home-header
	[:p "InventHealth.org [Cool graphics here]"]
	(link-to "/action/login" "Login")]
       [:div#home-about
	[:p "One paragraph intro to the site here or learn "
	 (link-to "/article/about" "more...")]]
       [:div#home-details
	[:div.home-detail
	 [:h2 "Discover Health Experiments"]
	 [:p "[copy about health discovery]"]]
	[:div.home-detail
	 [:h2 "Run a Self-Trial"]
	 [:p "[copy about your own trial]"]]
	[:div.home-detail
	 [:h2 "Design your own experiments"]
	 [:p "[copy about designing your own experiments]"]]
	[:div.home-detail
	 [:h2 "Share your experiences"]
	 [:p "You can journal privately or publically about your experiences.  You can comment and discuss any of the treatments with other people and integrate your experiments with social media"]]
	[:div.home-detail
	 [:h2 "Integrate with your devices"]
	 [:p "We integrate with the Zeo, your Garmin, Strava.com, your phone's SMS, and e-mail to simplify your self discovery process."]]
	[:div.home-detail
	 [:h2 "Support this service"]
	 [:p "We have a small grant to run this site, but we bear hosting costs and each SMS we sent cost a little money.  Donate [here] to the cause!"]]]
       [:div#home-footer
	(link-to "/article/terms" "Terms of Use") "|"
	(link-to "/article/privacy" "Privacy") "|"
	(link-to "/article/about" "About")]]))
	


