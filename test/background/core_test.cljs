(ns subman-chrome.background.core-test
  (:require-macros [cemerick.cljs.test :refer [deftest is use-fixtures done]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cemerick.cljs.test]
            [cljs.core.async :refer [>! <! chan timeout]]
            [subman-chrome.background.core :as b]))

(defn reset-atoms!
  []
  (reset! b/cache {})
  (reset! b/loading {})
  (reset! b/http-get nil))

(use-fixtures :each (fn [f] (reset-atoms!) (f) (reset-atoms!)))

(deftype ContextMenuMock [result-atom remove-all-atom]
  Object
  (create [_ menu] (swap! result-atom conj menu))
  (removeAll [_] (swap! remove-all-atom inc)))

(deftype TabsMock [create-atom]
  Object
  (create [_ data] (swap! create-atom conj data)))

(deftest test-create-context-menu
         (let [result (atom [])]
           (reset! b/context-menus (ContextMenuMock. result (atom 0)))
           (b/create-context-menu :contexts [:all]
                                  :title "Test Title")
           (is (= (js->clj @result :keywordize-keys true)
                  [{:contexts ["all"]
                    :title "Test Title"}]))))

(deftest test-get-menu-item-title
         (is (= (b/get-menu-item-title {:show "American Dad"
                                        :season 1
                                        :episode 12
                                        :source 0})
                "Addicted: American Dad S1E12"))
         (is (= (b/get-menu-item-title {:show "American Dad"
                                        :source 1})
                "Podnapisi: American Dad")))

(deftest test-menu-itme-from-subtitle
         (is (= (b/get-menu-item-title {:show "American Dad"
                                        :source 1
                                        :url "test-url"}))
             {:title "Podnapisi: American Dad"
              :url "test-url"}))

(deftest ^:async test-load-subtitles!
         (reset! b/http-get (fn [url]
                              (is (= url "http://subman.io/api/search/?query=title"))
                              (is (= @b/loading {"title" true}))
                              (let [ch (chan)]
                                (go (>! ch {:body (for [i (range 10)]
                                                    {:show (str "show-" i)
                                                     :source 1
                                                     :url (str "url-" i)})}))
                                ch)))
         (go (let [result (for [i (range b/result-limit)]
                            {:title (str "Podnapisi: show-" i)
                             :url (str "url-" i)})]
               (b/load-subtitles! ["title"])
               (<! (timeout 0))                             ; wait for `load-subtitles!`
               (is (= @b/cache {"title" result}))
               (is (= @b/loading {"title" false}))
               (done))))

(deftest test-on-clicked
         (let [result (atom [])]
           (reset! b/tabs (TabsMock. result))
           (b/on-clicked {:url "test-url"})
           (is (= (js->clj @result :keywordize-keys true)
                  [{:url "test-url"}]))))

(deftest test-get-menu-title
         (is (= (b/get-menu-title [:item] "title")
                "Subtitles"))
         (is (= (b/get-menu-title nil "title")
                "No subtitles found"))
         (swap! b/loading assoc "title" true)
         (is (= (b/get-menu-title nil "title")
                "Loading subtitles...")))

(deftest test-update-context-menu
         (let [menu (atom [])
               deleted (atom 0)]
           (reset! b/context-menus (ContextMenuMock. menu deleted))
           (swap! b/cache assoc "title" [{:title "menu item"
                                          :url "test-url"}])
           (b/update-context-menu {:with-menu? false
                                   :title "title"})
           (is (= 1 @deleted))
           (is (= [] @menu))
           (b/update-context-menu {:with-menu? true
                                   :title "title"})
           (is (= 2 @deleted))
           (is (= 2 (count @menu)))))
