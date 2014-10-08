(ns subman-chrome.shared.chrome
  (:require-macros [subman-chrome.shared.chrome :refer [defattr]])
  (:require [clj-di.core :refer [register!]]))

(defn available?
  "Is chrome available?"
  []
  (aget js/window "chrome"))

(defattr extension .-extension)

(defattr context-menus .-contextMenus)

(defattr tabs .-tabs)

(defn inject!
  "Inject real chrome into dependencies"
  []
  (register! :chrome-extension extension)
  (register! :chrome-context-menus context-menus)
  (register! :chrome-tabs tabs))
