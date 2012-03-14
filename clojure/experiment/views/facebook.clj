(ns experiment.views.facebook
  (:use
   noir.core
   hiccup.core)
  (:require
   [experiment.libs.properties :as props]))
        


;; # Facebook integration

(def facebook-init-script
  (format "window.fbAsyncInit = function () {
      FB.init({
            appId      : '%s',
            status     : true, 
            cookie     : true,
            xfbml      : true,
            oauth      : true,
          });
        };
        (function(d) {
           var js, id = 'facebook-jssdk'; if (d.getElementById(id)) {return;}
           js = d.createElement('script'); js.id = id; js.async = true;
           js.src = \"//connect.facebook.net/en_US/all.js\";
           d.getElementsByTagName('head')[0].appendChild(js);
         }(document));" (props/get :facebook.appid)))
                         
(defpartial include-jsapi []
  [:div#fb-root]
  [:script facebook-init-script])

;; # Facebook User Widgets

(defpartial login-button [title]
  [:div.fb-login-button {:data-scope "email"} title])

(defpartial register-button [target]
  [:div
   {:class "fb-registration"
    :data-scope "email"
    :data-fields "[{'name':'name'}, {'name':'email'}]"
    :data-redirect-uri target}])
