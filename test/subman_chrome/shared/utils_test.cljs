(ns subman-chrome.shared.utils-test
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]])
  (:require [cemerick.cljs.test]
            [subman-chrome.shared.utils :as u]))

(deftest test-is-filled?
         (testing "not when nil"
                  (is (false?  (u/is-filled? nil))))
         (testing "not when blank"
                  (is (false? (u/is-filled? ""))))
         (testing "or yes"
                  (is (true? (u/is-filled? "test")))))

(deftest test-add-0-if-need
         (testing "if length = 1"
                  (is (= "03" (u/add-0-if-need "3"))))
         (testing "if length = 1 and number passed"
                  (is (= "03" (u/add-0-if-need 3))))
         (testing "not if other lenght"
                  (is (= "12" (u/add-0-if-need "12")))))
