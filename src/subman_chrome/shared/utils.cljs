(ns subman-chrome.shared.utils)

(defn is-filled?
  "Is field filled"
  [value]
  (and (not (nil? value))
       (not= "" value)))

(defn add-0-if-need
  "Add 0 before number if need"
  [number]
  (if (= (count (str number)) 1)
    (str "0" number)
    number))
