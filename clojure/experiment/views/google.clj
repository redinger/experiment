(ns experiment.views.google
  (:require
   [experiment.libs.properties :as props]))

;; # Google Analytics
(def analytics-script
  "Create the google analytics script from site properties"
  (format "<script type=\"text/javascript\">

  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', '%s']);
  _gaq.push(['_setDomainName', '%s']);
  _gaq.push(['_setAllowLinker', true]);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();

</script>"
          (props/get :google.analytics.id)
          (props/get :google.analytics.dom)))

(defn include-analytics
  "Use in production mode only"
  []
  (when (= (props/get :mode) :prod)
    analytics-script))
