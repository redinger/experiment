(ns experiment.models.core
  (:use [experiment.infra.models]))

;; VARIABLE/SYMPTOM [name ref]
;;  has name
;;  contains Comments

;; TREATMENT [name ref]
;;  has name
;;  has tags[]
;;  has averageRating
;;  server ratings{user: value}
;;  contains Comments
;;  - op: tag
;;  - op: comment
;;  - op: rate (send to server, update average)

;; TREATMENT-REF
;;  refs Treatment.name

;; INSTRUMENT [type ref]
;;  has name
;;  has type
;;  refs Variable.name
;;  has implementedp -- new instrument objects are requests
;;  contains Comments

;; EXPERIMENT
;;  refs User.username 
;;  has title
;;  has instructions
;;  contains Schedule
;;  contains TreatmentRefs[]
;;  ref Instrument[]
;;  has tags[]
;;  contains Ratings{}
;;  contains Comments[]
;;  - op: tag
;;  - op: comment
;;  - op: rate (send to server, update average)

;; TRIAL
;;  refs User
;;  refs Experiment
;;  has outcome #[notstart, abandon, success, fail, uncertain]

;; JOURNAL
;;  refs User
;;  refs Object [Trial, Experiment, Treatment]
;;  

;; COMMENT
;;  refs User
;;  has upVotes
;;  has downVotes
;;  has title
;;  has content

;; SCHEDULE