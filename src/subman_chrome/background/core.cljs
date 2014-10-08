(ns subman-chrome.background.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [clj-di.core :refer [let-deps]])
  (:require [clojure.string :as string]
            [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [clj-di.core :refer [register! get-dep]]
            [subman-chrome.shared.chrome :as c]))

(def result-limit 5)

(defn create-context-menu
  [& params]
  (let-deps [context-menus :chrome-context-menus]
    (->> (apply hash-map params)
         clj->js
         (.create context-menus))))

(def sources {0 "Addicted"
              1 "Podnapisi"
              2 "OpenSubtitles"
              3 "Subscene"
              4 "Notabenoid"
              5 "UKsubtitles"})

(defn get-menu-item-title
  "Get title for single menu item."
  [{:keys [show season episode source]}]
  (let [season-episode (if (and season episode) (str " S" season "E" episode) "")
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
  (let-deps [http-get :http-get
             cache :cache
             loading :loading]
    (doseq [title titles]
      (go (swap! loading assoc title true)
          (->> (str "http://subman.io/api/search/?query=" title)
               http-get
               <!
               :body
               (take result-limit)
               (map menu-item-from-subtitle)
               (swap! cache assoc title))
          (swap! loading assoc title false)))))

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
  (c/inject!)
  (register! :http-get http/get)
  (register! :cache (atom {}))
  (register! :loading (atom {}))
  (let-deps [extension :chrome-extension]
    (.. extension -onMessage (addListener message-listener))))
