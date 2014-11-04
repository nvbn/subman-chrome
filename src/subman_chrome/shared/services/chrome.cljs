(ns subman-chrome.shared.services.chrome
  (:require-macros [clj-di.core :refer [defprotocol*]]))

(defn available?
  "Is chrome available?"
  []
  (aget js/window "chrome"))

(defprotocol* chrome
  (on-message [_ listener])
  (send-message [_ params])
  (set-page-icon [_ params])
  (set-page-title [_ params])
  (show-page-action [_ tab])
  (remove-context-menus [_])
  (create-context-menu [_ params])
  (create-tab [_ url]))

(deftype chrome-impl []
  chrome
  (on-message [_ listener] (.. js/chrome -extension -onMessage (addListener listener)))
  (send-message [_ params] (.. js/chrome -extension (sendMessage (clj->js params))))
  (set-page-icon [_ params] (.. js/chrome -pageAction (setIcon (clj->js params))))
  (set-page-title [_ params] (.. js/chrome -pageAction (setTitle (clj->js params))))
  (show-page-action [_ tab] (.. js/chrome -pageAction (show (.-id tab))))
  (remove-context-menus [_] (.. js/chrome -contextMenus (removeAll)))
  (create-context-menu [_ params] (.. js/chrome -contextMenus (create (clj->js params))))
  (create-tab [_ url] (.. js/chrome -tabs (create #js {:url url}))))
