(ns scoir.core-test
  (:require [clojure.test :refer :all]
            [scoir.core :refer :all])
  (:import (mock MockWriter)))

(deftest t-read-parse
  (testing "Given an error free first row, then start a json array"
    (let [result (:output (read-parse ["12345678" "Chris" "" "Ryan" "610-555-8888"] 1 false))]
      (is (= \[ (.charAt result 0)))
      ))
  (testing "Given an error free row that is not the first, then prepend a comma"
    (let [result (:output (read-parse ["12345678" "Chris" "" "Ryan" "610-555-8888"] 1 true))]
      (is (= \, (.charAt result 0)))))
  (testing "Given an error free row, then there should be some output and no errors"
    (let [result (read-parse ["12345678" "Chris" "" "Ryan" "610-555-8888"] 2 true)
          output (:output result)
          errors (:errors result)]
      (is (some? output))
      (is (nil? errors))))
  (testing "Given a row with errors, then there should be no output and at least 1 error"
    (let [result (read-parse ["1" "Chris" "" "Ryan" "610-555-8888"] 2 true)
          output (:output result)
          errors (:errors result)]
      (is (nil? output))
      (is (some? errors))))
  (testing "The output should be json"
    (let [result (read-parse ["12345678" "Chris" "" "Ryan" "610-555-8888"] 2 true)
          output (:output result)]
      (is (.contains output "{"))
      (is (.contains output "}"))))
  (testing "If there is no middle name, remove it from the output."
    (let [result (read-parse ["12345678" "Chris" "" "Ryan" "610-555-8888"] 2 true)
          output (:output result)]
      (is (not (contains? (:name output) :middle)))
      ))
  (testing "If there is a middle name, it should be in the output."
    (let [result (read-parse ["12345678" "Chris" "Patrick" "Ryan" "610-555-8888"] 2 true)
          output (:output result)]
      (is (.contains output "middle"))
      (is (.contains output "Patrick")))))

(deftest t-all-error-msgs
  (testing "Internal ID validations"
    (let [internal-id "INTERNAL_ID"
          csv-map {:id     ""
                   :first  "Chris"
                   :middle ""
                   :last   "Ryan"
                   :phone  "610-952-8688"}
          errors (all-error-msgs csv-map)]
      (is (= 3 (count errors)))
      (is (= (first errors) (m-equal-digits internal-id 8)))
      (is (= (second errors) (m-is-string-int internal-id)))
      (is (= (nth errors 2) (m-not-empty internal-id)))))
  (testing "First name validations"
    (let [first-name "FIRST_NAME"]
      (let [csv-map {:id     "12345678"
                     :first  ""
                     :middle ""
                     :last   "Ryan"
                     :phone  "610-952-8688"}
            errors (all-error-msgs csv-map)]
        (is (= 1 (count errors)))
        (is (= (first errors) (m-not-empty first-name))))
      (let [csv-map {:id     "12345678"
                     :first  "ChrisChrisChris1"             ;16
                     :middle ""
                     :last   "Ryan"
                     :phone  "610-952-8688"}
            errors (all-error-msgs csv-map)]
        (is (= 1 (count errors)))
        (is (= (first errors) (m-max-length first-name 15))))))
  (testing "Middle name validations"
    (let [middle-name "MIDDLE_NAME"
          csv-map {:id     "12345678"
                   :first  "Chris"
                   :middle "PatrickPatrick12" ;16
                   :last   "Ryan"
                   :phone  "610-952-8688"}
          errors (all-error-msgs csv-map)]
      (is (= 1 (count errors)))
      (is (= (first errors) (m-max-length middle-name 15)))))
  (testing "Last name validations"
    (let [last-name "LAST_NAME"]
      (let [csv-map {:id     "12345678"
                     :first  "Chris"
                     :middle ""
                     :last   ""
                     :phone  "610-952-8688"}
            errors (all-error-msgs csv-map)]
        (is (= 1 (count errors)))
        (is (= (first errors) (m-not-empty last-name))))
      (let [csv-map {:id     "12345678"
                     :first  "Chris"
                     :middle ""
                     :last   "RyanRyanRyanRyan"             ;16
                     :phone  "610-952-8688"}
            errors (all-error-msgs csv-map)]
        (is (= 1 (count errors)))
        (is (= (first errors) (m-max-length last-name 15))))))
  (testing "Phone validations"
    (let [phone "PHONE_NUM"]
      (let [csv-map {:id     "12345678"
                     :first  "Chris"
                     :middle ""
                     :last   "Ryan"
                     :phone  ""}
            errors (all-error-msgs csv-map)]
        (is (= 2 (count errors)))
        (is (= (first errors) (m-phone-format phone)))
        (is (= (second errors) (m-not-empty phone)))))))

(deftest t-read-parse-write!
  (testing "End of array is written to the out writer."
    (let [out-writer (MockWriter.)]
      (read-parse-write! [["12345678" "Chris" "" "Ryan" "610-952-8688"]] out-writer (MockWriter.))
      (is (= \] (.charAt (.getContent out-writer)
                         (dec (.length (.getContent out-writer))))))))
  (testing "Errors get written to the error writer."
    (let [error-writer (MockWriter.)]
      (read-parse-write! [["" "Chris" "" "Ryan" "610-952-8688"]] (MockWriter.) error-writer)
      (is (not (zero? (.length (.getContent error-writer))))))
    ))



