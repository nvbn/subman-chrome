(ns subman-chrome.background.core-test
  (:require-macros [cemerick.cljs.test :refer [deftest is use-fixtures done]]
                   [cljs.core.async.macros :refer [go]]
                   [clj-di.test :refer [with-fresh-dependencies]])
  (:require [cemerick.cljs.test]
            [cljs.core.async :refer [>! <! chan timeout]]
            [clj-di.core :refer [register! get-dep dependencies]]
            [cljs-http.client :as http]
            [subman-chrome.shared.const :as const]
            [subman-chrome.shared.services.chrome :as c]
            [subman-chrome.background.models :as m]
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

(deftest test-get-menu-item-title
         (register-default-sources!)
         (is (= (b/get-menu-item-title {:show "American Dad"
                                        :season 1
                                        :episode 12
                                        :source 0})
                "American Dad S01E12 (Addicted)"))
         (is (= (b/get-menu-item-title {:show "American Dad"
                                        :source 1})
                "American Dad (Podnapisi)")))

(deftest test-menu-itme-from-subtitle
         (register-default-sources!)
         (is (= (b/get-menu-item-title {:show "American Dad"
                                        :source 1
                                        :url "test-url"}))
             {:title "American Dad (Podnapisi)"
              :url "test-url"}))

(deftest ^:async test-load-subtitles!
         (register! :cache (atom {})
                    :loading (atom {})
                    :options (atom const/default-options)
                    :models (reify m/models
                              (get-subtitles [_ titles limit lang source]
                                (is (= @(get-dep :loading) {"title" true}))
                                (is (= titles ["title"]))
                                (is (= limit const/result-limit))
                                (is (= lang const/default-lang))
                                (is (= source const/all-sources-id))
                                (go {"title" (for [i (range 5)]
                                               {:show (str "show-" i)
                                                :source 1
                                                :url (str "url-" i)})}))
                              (get-source-id [_ source] (m/-get-source-id source))))
         (register-default-sources!)
         (go (let [result (for [i (range const/result-limit)]
                            {:title (str "show-" i " (Podnapisi)")
                             :url (str "url-" i)})
                   cache (get-dep :cache)
                   loading (get-dep :loading)]
               (b/load-subtitles! ["title"])
               (<! (timeout 0))                             ; wait for `load-subtitles!`
               (is (= @cache {"title" result}))
               (is (= @loading {"title" false})))
             (done)))

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
           (register! :chrome (reify c/chrome
                                (create-context-menu [_ params] (swap! menu conj params))
                                (remove-context-menus [_] (swap! deleted inc))))
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
