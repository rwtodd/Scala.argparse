(ns org-rwtodd.argparse.core-test
  (:require [clojure.test :refer :all]
            [org-rwtodd.argparse.core :as arg]))


(deftest test-counter
  (testing "Counting args"
    (let [spec { :verbose (arg/counter-param "how verbose?" :short \v) }]
      (is (== (:verbose (arg/parse spec ["-vvv"])) 3))
      (is (== (:verbose (arg/parse spec [""])) 0))
      (is (== (:verbose (arg/parse spec ["-vvv" "--verbose" "--verbose"])) 5)))))

(deftest test-validator
  (testing "Valid int ranges"
    (let [spec { :count (arg/int-param "NUM" "How many?" :validator #(<= 0 % 5)) }]
      (is (== (:count (arg/parse spec ["--count=3"])) 3))
      (is (thrown? IllegalArgumentException (arg/parse spec ["--count=6"]))))))

(deftest test-parsing
  (let [spec { :count (arg/int-param "NUM" "How many?") }]
    (testing "the -- flag"
      (is (nil? (:count (arg/parse spec ["--" "--count=21"]))))
      (is (= ["--count=21"] (:free-args (arg/parse spec ["--" "--count=21"])))))))
