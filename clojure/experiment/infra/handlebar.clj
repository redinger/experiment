(ns experiment.infra.handlebar
  (:use handlebars.templates
        experiment.infra.api
        noir.core)
  (:require [noir.response :as response]))

;; A handler to define a template renderer that we can
;; evalute on the server w/ an object or render to the client
;; as a script template (or dynamically via a loader?)

(defpage get-templates [:get "/api/template/get/:name"]
  {:keys [name]}
  (response/content-type
   "text/x-jquery-html"
   (html-template (get-template name))))
