(ns experiment.libs.mail
  (:require [postal.core :as post]
            [experiment.libs.properties :as props]))
            
(def mailer (agent [0 0]))

(def admin-address* (or (props/get :admin-address true)
                        "admin@personalexperiments.org"))
  

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

(defn send-message-to [message email]
  (assert (every? #(get message %) [:subject :body]))
  (send mailer send-message (merge {:to email
                                    :from admin-address*}
                                   message)))

(defn send-message-to-group [message group]
  (assert (and (every? #(get message %) [:subject :body])
               (sequential? group)))
  (send mailer send-message (merge {:to admin-address*
                                    :bcc group
                                    :from admin-address*}
                                   message)))
  

(defn send-site-message [message]
  (assert (every? #(get message %) [:subject :body]))
  (send mailer send-message (merge {:to "ianeslick@gmail.com"
                                    :from "admin@personalexperiments.org"}
                                   message)))
