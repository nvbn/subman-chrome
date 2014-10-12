(ns subman-chrome.shared.chrome
  (:require-macros [subman-chrome.shared.chrome :refer [defattr]])
  (:require [clj-di.core :refer [register!]]))

(defn available?
  "Is chrome available?"
  []
  (aget js/window "chrome"))

(defattr extension extension)

(defattr context-menus contextMenus)

(defattr tabs tabs)

(defattr page-action pageAction)

(defn inject!
  "Inject real chrome into dependencies"
  []
  (register! :chrome-extension extension
             :chrome-context-menus context-menus
             :chrome-tabs tabs
             :chrome-page-action page-action))
