(ns subman-chrome.background.models-test
  (:require-macros [cemerick.cljs.test :refer [deftest is testing done]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cemerick.cljs.test]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [subman-chrome.shared.const :as const]
            [subman-chrome.background.models :as m]))

(deftest ^:async test-get-sources
         (go (with-redefs [http/get (fn [_] (go {:status 200
                                                 :body {0 "Opensubtitles"
                                                        1 "UKSubtitles"}}))]
               (testing "when status is 200"
                        (is (= (<! (m/get-sources)) {0 "opensubtitles"
                                                     1 "uksubtitles"}))))
             (testing "when not 200 first time"
                      (let [flag (atom false)]
                        (with-redefs [http/get (fn [_] (go (if @flag
                                                             {:status 200
                                                              :body {}}
                                                             (do (reset! flag true)
                                                                 {:status 400}))))]
                          (is (= (<! (m/get-sources)) {}))
                          (done))))))

(deftest test-get-source-id
         (testing "when source = all"
                  (is (= (m/get-source-id const/default-sources const/all-sources)
                         const/all-sources-id)))
         (testing "when ordinary source"
                  (is (= (m/get-source-id const/default-sources "notabenoid")
                         4))))

(deftest ^:async test-get-subtitles
         (go (with-redefs [http/post (fn [_ {:keys [transit-params]}]
                                       (go (is (= transit-params {:queries ["dads" "simpsons"]
                                                                  :limit const/result-limit
                                                                  :lang const/default-lang
                                                                  :source const/all-sources-id}))
                                           {:body :subtitles}))]
               (is (= :subtitles (<! (m/get-subtitles ["dads" "simpsons"]
                                                      const/result-limit
                                                      const/default-lang
                                                      const/all-sources-id))))
               (done))))
