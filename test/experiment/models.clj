(ns experiment.models
  (:use clojure.test
        experiment.setup
        experiment.infra.models))

(use-fixtures :once clean-database-fixture)


(def john-template
  {:type "user"
   :name "John Doe"
   :username "jdoe"
   :password "foo"
   :email "someone@nowhere.com"})

(defn get-john []
  (fetch-model {:type "user" :username (:username john-template)}))

(defn create-john []
  (if-let [john (get-john)]
    john
    (create-model! john-template)))

(defn without-id [model]
  (dissoc model :_id))

(defn john? [obj]
  (= john-template (select-keys obj (keys john-template))))

;; ## CRUD TESTING


(deftest test-model-create []
  (let [_ (when-let [j (get-john)] (delete-model! j))
        result (create-john)]
    (is (objectid? (:_id result)))
    (is (= john-template (without-id result)))
    (is (objectid? (:_id (get-john))))
    (is (= john-template (without-id (get-john))))))

(deftest test-model-update []
  (create-john)
  (update-model! (assoc (get-john) :newfield "testing"))
  (is (= (:newfield (get-john)) "testing")))

(deftest test-model-modify []
  (create-john)
  (modify-model! (get-john) {:$set {:newfield2.foo "testing"}})
  (is (= (get-in (get-john) [:newfield2 :foo]) "testing")))

(deftest test-model-delete []
  (create-john)
  (delete-model! (get-john))
  (is (nil? (fetch-model {:type "user" :username "jdoe"}))))

(deftest test-model-fetch-options []
  (create-john)
  (is (john? (fetch-model :user {:username "jdoe"})))
  (is (john? (fetch-model "user" {:username "jdoe"})))
  (is (john? (fetch-model {:type "user" :username "jdoe"})))
  (is (john? (fetch-model :user :where {:username "jdoe"})))
  (is (let [result (fetch-model :user :only [:username] :where {:username "jdoe"})]
        (and (= 2 (count (keys result)))
             (= "jdoe" (:username result))))))
           