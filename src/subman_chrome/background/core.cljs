(ns subman-chrome.background.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [clj-di.core :refer [let-deps]])
  (:require [clojure.string :as string]
            [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [clj-di.core :refer [register! get-dep]]
            [subman-chrome.shared.const :as const]
            [subman-chrome.shared.chrome :as c]
            [subman-chrome.shared.utils :as u]))

(defn create-context-menu
  [& params]
  (let-deps [context-menus :chrome-context-menus]
    (->> (apply hash-map params)
         clj->js
         (.create context-menus))))

(defn get-menu-item-title
  "Get title for single menu item."
  [{:keys [show season episode source]}]
  (let [season-episode (if (some u/is-filled? [season episode])
                         (str " S" (u/add-0-if-need season)
                              "E" (u/add-0-if-need episode))
                         "")
        sources (get-dep :sources)
        source (sources source)]
    (string/replace (str source ": " show season-episode)
                    #"(\t|\n|\r)" " ")))

(defn menu-item-from-subtitle
  "Transforms subtitle to menu item."
  [{:keys [url] :as subtitle}]
  {:title (get-menu-item-title subtitle)
   :url url})

(defn load-subtitles!
  "Loads subtitles from server."
  [titles]
  (go (let-deps [cache :cache
                 loading :loading]
        (doseq [title titles] (swap! loading assoc title true))
        (let [result (->> (http/post const/search-url
                                     {:transit-params {:queries titles
                                                       :limit const/result-limit}})
                          <!
                          :body)]
          (doseq [[title value] result]
            (swap! loading assoc title false)
            (when (seq value)
              (swap! cache assoc title
                     (map menu-item-from-subtitle value))))))))

(defn on-clicked
  "Open new tab with subtitle."
  [{:keys [url]}]
  (let-deps [tabs :chrome-tabs]
    (.create tabs #js {:url url})))

(defn get-menu-title
  "Get title for main context menu entry."
  [items title]
  (cond
    items "Subtitles"
    (@(get-dep :loading) title) "Loading subtitles..."
    :else "No subtitles found"))

(defn update-context-menu
  "Update context menu when episode hovered."
  [{:keys [with-menu? title]}]
  (let-deps [context-menus :chrome-context-menus
             cache :cache]
    (.removeAll context-menus)
    (when with-menu?
      (let [items (seq (@cache title))]
        (create-context-menu :contexts [:link]
                             :id :subtitles-menu
                             :title (get-menu-title items title))
        (doseq [item items]
          (create-context-menu :contexts [:link]
                               :parentId :subtitles-menu
                               :title (:title item)
                               :onclick (partial on-clicked item)))))))

(defn message-listener
  "Handle messages from content."
  [msg _ _]
  (let [msg (js->clj msg :keywordize-keys true)]
    (condp = (keyword (:request msg))
      :load-subtitles (load-subtitles! (:titles msg))
      :update-context-menu (update-context-menu (:data msg)))))

(when (c/available?)
  (go (c/inject!)
      (register! :cache (atom {})
                 :loading (atom {})
                 :sources (:body (<! (http/get const/sources-url))))
      (let-deps [extension :chrome-extension]
        (.. extension -onMessage (addListener message-listener)))))
