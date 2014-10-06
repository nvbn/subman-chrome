(ns subman-chrome.content.core-test
  (:require-macros [cemerick.cljs.test :refer [deftest is use-fixtures]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cemerick.cljs.test]
            [cljs.core.async :refer [<!]]
            [subman-chrome.content.core :as c]))

(deftype ExtensionMock [result-atom]
  Object
  (sendMessage [_ msg] (reset! result-atom msg)))

(deftype ChromeMock [extension]
  Object)

(defn clear!
  "Clear page."
  []
  (set! (.-innerHTML (.-body js/document))) "")

(use-fixtures :each (fn [f] (clear!) (f) (clear!)))

(deftest test-send-message
         (let [result (atom nil)]
           (reset! c/chrome (ChromeMock. (ExtensionMock. result)))
           (c/send-message :request :test-request
                           :data {:id 1
                                  :name "test"})
           (is (= (js->clj @result :keywordize-keys true)
                  {:request "test-request"
                   :data {:id 1
                          :name "test"}}))))

(deftest test-get-titles
         (js/document.write "<a class='epinfo'>American Dad</a>
                             <a class='epinfo'>Family Guy</a>
                             <a class='detLink'>Simpsons</a>")
         (is (= (c/get-titles "epinfo") ["American Dad" "Family Guy"])))

(deftest test-with-menu?
         (js/document.write "<a id='with-menu' class='epinfo'></a>
                             <a id='without-menu' class='detLink'></a>")
         (is (true? (c/with-menu? (.getElementById js/document "with-menu")
                                  "epinfo")))
         (is (false? (c/with-menu? (.getElementById js/document "without-menu")
                                   "epinfo"))))
