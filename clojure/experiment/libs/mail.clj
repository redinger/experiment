(ns experiment.libs.mail
  (:require [postal.core :as post]))

(defn send-message [message]
  (post/send-message
   #^{:host "smtp.gmail.com"
      :user "ianeslick@gmail.com"
      :pass "3eraGOOGH8dr"
      :ssl true}
   message))

(defn send-message-to [email message]
  (assert (every? #(get message %) [:subject :body]))
  (send-message (merge {:to email
                        :from "ianeslick@gmail.com"}
                       message)))

(defn send-site-message [message]
  (assert (every? #(get message %) [:subject :body]))
  (send-message (merge {:to "ianeslick@gmail.com"
                        :from "ianeslick@gmail.com"}
                       message)))
                  
                        

