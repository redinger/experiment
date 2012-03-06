(ns experiment.libs.mail
  (:require [postal.core :as post]
            [experiment.libs.properties :as props]))
            
(def mailer (agent [0 0]))

(defn send-message [[count errors] message]
  (try
    (do (post/send-message
         #^{:host (props/get :mail.host)
            :user (props/get :mail.user)
            :pass (props/get :mail.pass) 
            :ssl (> (count (props/get :mail.ssl)) 0)}
         message)
        [(inc count) errors])
    (catch java.lang.Throwable e
      [count (inc errors)])))

(defn send-message-to [email message]
  (assert (every? #(get message %) [:subject :body]))
  (send mailer send-message (merge {:to email
                                    :from "admin@personalexperiments.org"}
                                   message)))

(defn send-site-message [message]
  (assert (every? #(get message %) [:subject :body]))
  (send mailer send-message (merge {:to "ianeslick@gmail.com"
                                    :from "admin@personalexperiments.org"}
                                   message)))
