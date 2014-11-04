(ns subman-chrome.background.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [clj-di.core :refer [let-deps]])
  (:require [clojure.string :as string]
            [cljs.core.async :refer [<! timeout]]
            [clojure.set :refer [map-invert]]
            [clj-di.core :refer [register! get-dep]]
            [alandipert.storage-atom :refer [local-storage]]
            [subman-chrome.shared.services.http :refer [http-client-impl]]
            [subman-chrome.shared.const :as const]
            [subman-chrome.shared.services.chrome :as c]
            [subman-chrome.shared.utils :as u]
            [subman-chrome.background.models :as m]))

(defn get-menu-item-title
  "Get title for single menu item."
  [{:keys [show season episode source]}]
  (let [season-episode (if (some u/is-filled? [season episode])
                         (str " S" (u/add-0-if-need season)
                              "E" (u/add-0-if-need episode))
                         "")
        sources (get-dep :sources)
        source (sources source)]
    (string/replace (str show season-episode " (" source ")")
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
                 loading :loading
                 options :options
                 sources :sources]
        (doseq [title titles] (swap! loading assoc title true))
        (doseq [[title value] (<! (m/get-subtitles* titles
                                                    const/result-limit
                                                    (:language @options)
                                                    (m/get-source-id* (:source @options))))]
          (swap! loading assoc title false)
          (when (seq value)
            (swap! cache assoc title
                   (map menu-item-from-subtitle value)))))))

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
  (let-deps [cache :cache]
    (c/remove-context-menus*)
    (when with-menu?
      (let [items (seq (@cache title))]
        (c/create-context-menu* {:contexts [:link]
                                 :id :subtitles-menu
                                 :title (get-menu-title items title)})
        (doseq [item items]
          (c/create-context-menu* {:contexts [:link]
                                   :parentId :subtitles-menu
                                   :title (:title item)
                                   :onclick (partial c/create-tab* (:url item))}))))))

(defn with-icon
  "Show loading icon and then show normal icon."
  [tab ch]
  (go (c/show-page-action* tab)
      (c/set-page-icon* {:path "loading_48_48.png"
                         :tabId (.-id tab)})
      (c/set-page-title* {:title "Loading subtitles..."
                          :tabId (.-id tab)})
      (<! ch)
      (c/set-page-icon* {:path "icon_48_48.png"
                         :tabId (.-id tab)})
      (c/set-page-title* {:title "Right click on link to episode for seeing subtitles."
                          :tabId (.-id tab)})))

(defn update-options!
  "Update options in local storage."
  [options]
  (let-deps [storage :options]
    (reset! storage options)))

(defn message-listener
  "Handle messages from content."
  [msg sender _]
  (let [msg (js->clj msg :keywordize-keys true)]
    (condp = (keyword (:request msg))
      :load-subtitles (with-icon (.-tab sender)
                                 (load-subtitles! (:titles msg)))
      :update-context-menu (update-context-menu (:data msg))
      :update-options (update-options! (:options msg)))))

(when (c/available?)
  (register! :cache (atom {})
             :loading (atom {})
             :http-client (http-client-impl.)
             :models (m/models-impl.)
             :options (local-storage (atom const/default-options)
                                     :options)
             :chrome (c/chrome-impl.))
  (go (register! :sources (<! (m/get-sources*)))
      (c/on-message* message-listener)))
