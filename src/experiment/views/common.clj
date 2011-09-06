(ns experiment.views.common
  (:use noir.core
        noir.content.css
        hiccup.core
        hiccup.page-helpers))

(defpartial layout [& content]
  (html5
   [:head
    [:title "Experiment Market"]
    (include-css "/css/reset.css")
    [:style {:type "text/css"} (noir-css)]]
   [:body
    [:div#wrapper
     content]]))
