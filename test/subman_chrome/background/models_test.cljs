(ns subman-chrome.background.models-test
  (:require-macros [cemerick.cljs.test :refer [deftest is testing done]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cemerick.cljs.test]
            [clj-di.core :refer [register!]]
            [cljs.core.async :refer [<! timeout]]
            [subman-chrome.shared.services.http :as http]
            [subman-chrome.shared.const :as const]
            [subman-chrome.background.models :as m]))

(deftest ^:async test-get-sources-when-status-is-200
         (register! :http-client (reify http/http-client
                                   (get [_ _] (go {:status 200
                                                   :body {0 "Opensubtitles"
                                                          1 "UKSubtitles"}}))))
         (go (is (= (<! (m/-get-sources)) {0 "opensubtitles"
                                           1 "uksubtitles"}))
             (done)))

(deftest ^:async test-get-sources-when-not-200
         (let [flag (atom false)]
           (register! :http-client (reify http/http-client
                                     (get [_ _] (go (if @flag
                                                      {:status 200
                                                       :body {}}
                                                      (do (reset! flag true)
                                                          {:status 400}))))))
           (go (is (= (<! (m/-get-sources)) {}))
               (done))))

(deftest test-get-source-id
         (register! :sources const/default-sources )
         (testing "when source = all"
                  (is (= (m/-get-source-id const/all-sources)
                         const/all-sources-id)))
         (testing "when ordinary source"
                  (is (= (m/-get-source-id "notabenoid")
                         4))))

(deftest ^:async test-get-subtitles
         (register! :http-client (reify http/http-client
                                   (post [_ _ {:keys [transit-params]}]
                                     (go (is (= transit-params {:queries ["dads" "simpsons"]
                                                                :limit const/result-limit
                                                                :lang const/default-lang
                                                                :source const/all-sources-id}))
                                         {:body :subtitles}))))
         (go (is (= :subtitles (<! (m/-get-subtitles ["dads" "simpsons"]
                                                     const/result-limit
                                                     const/default-lang
                                                     const/all-sources-id))))
             (done)))
