(ns experiment.infra.handlebar
  (:require [handlebars.templates]))

;; A handler to define a template renderer that we can
;; evalute on the server w/ an object or render to the client
;; as a script template (or dynamically via a loader?)

(defmacro defhandlebar [name])
			
