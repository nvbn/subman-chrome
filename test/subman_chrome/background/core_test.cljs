(ns subman-chrome.background.core-test
  (:require-macros [cemerick.cljs.test :refer [deftest is use-fixtures done]]
                   [cljs.core.async.macros :refer [go]]
                   [clj-di.test :refer [with-fresh-dependencies]])
  (:require [cemerick.cljs.test]
            [cljs.core.async :refer [>! <! chan timeout]]
            [clj-di.core :refer [register! get-dep dependencies]]
            [cljs-http.client :as http]
            [subman-chrome.shared.const :as const]
            [subman-chrome.background.core :as b]))

(use-fixtures :each
              (fn [f] (with-fresh-dependencies (f))))

(defn register-default-sources!
  []
  (register! :sources {0 "Addicted"
                       1 "Podnapisi"
                       2 "OpenSubtitles"
                       3 "Subscene"
                       4 "Notabenoid"
                       5 "UKsubtitles"}))

(deftype ContextMenuMock [result-atom remove-all-atom]
  Object
  (create [_ menu] (swap! result-atom conj menu))
  (removeAll [_] (swap! remove-all-atom inc)))

(deftype TabsMock [create-atom]
  Object
  (create [_ data] (swap! create-atom conj data)))

(deftest test-create-context-menu
         (let [result (atom [])]
           (register! :chrome-context-menus (ContextMenuMock. result (atom 0)))
           (b/create-context-menu :contexts [:all]
                                  :title "Test Title")
           (is (= (js->clj @result :keywordize-keys true)
                  [{:contexts ["all"]
                    :title "Test Title"}]))))

(deftest test-get-menu-item-title
         (register-default-sources!)
         (is (= (b/get-menu-item-title {:show "American Dad"
                                        :season 1
                                        :episode 12
                                        :source 0})
                "Addicted: American Dad S01E12"))
         (is (= (b/get-menu-item-title {:show "American Dad"
                                        :source 1})
                "Podnapisi: American Dad")))

(deftest test-menu-itme-from-subtitle
         (register-default-sources!)
         (is (= (b/get-menu-item-title {:show "American Dad"
                                        :source 1
                                        :url "test-url"}))
             {:title "Podnapisi: American Dad"
              :url "test-url"}))

(deftest ^:async test-load-subtitles!
         (register! :cache (atom {})
                    :loading (atom {}))
         (register-default-sources!)
         (go (with-redefs [http/post (fn [& _]
                                       (is (= @(get-dep :loading) {"title" true}))
                                       (let [ch (chan)]
                                         (go (>! ch {:body {"title" (for [i (range 5)]
                                                                      {:show (str "show-" i)
                                                                       :source 1
                                                                       :url (str "url-" i)})}}))
                                         ch))]
               (let [result (for [i (range const/result-limit)]
                              {:title (str "Podnapisi: show-" i)
                               :url (str "url-" i)})
                     cache (get-dep :cache)
                     loading (get-dep :loading)]
                 (b/load-subtitles! ["title"])
                 (<! (timeout 0))                           ; wait for `load-subtitles!`
                 (is (= @cache {"title" result}))
                 (is (= @loading {"title" false}))
                 (done)))))

(deftest test-on-clicked
         (let [result (atom [])]
           (register! :chrome-tabs (TabsMock. result))
           (b/on-clicked {:url "test-url"})
           (is (= (js->clj @result :keywordize-keys true)
                  [{:url "test-url"}]))))

(deftest test-get-menu-title
         (register! :loading (atom {}))
         (is (= (b/get-menu-title [:item] "title")
                "Subtitles"))
         (is (= (b/get-menu-title nil "title")
                "No subtitles found"))
         (swap! (get-dep :loading) assoc "title" true)
         (is (= (b/get-menu-title nil "title")
                "Loading subtitles...")))

(deftest test-update-context-menu
         (register! :cache (atom {}))
         (let [menu (atom [])
               deleted (atom 0)
               cache (get-dep :cache)]
           (register! :chrome-context-menus (ContextMenuMock. menu deleted))
           (swap! cache assoc "title" [{:title "menu item"
                                        :url "test-url"}])
           (b/update-context-menu {:with-menu? false
                                   :title "title"})
           (is (= 1 @deleted))
           (is (= [] @menu))
           (b/update-context-menu {:with-menu? true
                                   :title "title"})
           (is (= 2 @deleted))
           (is (= 2 (count @menu)))))
