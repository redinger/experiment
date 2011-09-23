(ns experiment.views.common
  (:use noir.core
        noir.content.css
        hiccup.core
        hiccup.page-helpers))

(defpartial layout [& content]
  (html5
   ;; Header
   [:head
    [:title "MyExperiment Market"]
    (include-css "/css/reset.css")
    [:style {:type "text/css"} (noir-css)]]
   ;; Body
   [:body
    [:div#wrapper
     content]]
   ;; Scripts
   (include-js "/js/jquery.js"
	       "/js/handlebars.1.0.0.beta.3.js"
	       "/js/underscore.js"
	       "/js/backbone.js"
	       "/js/test1.js")))

