(ns experiment.jobs.sampling
  (:require [quartz-clj.core :as q]))

(q/defjob experiment.jobs.sampling [context]
  (let [jobId (.get (.getJobDataMap (.getJobDetail context)) "jobId")]
    (println (format "Job firing with jobId=[%s]" jobId))))
