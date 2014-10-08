(ns subman-chrome.shared.chrome)

(defmacro defattr
  [name getter]
  `(def ~name
     (when (subman-chrome.shared.chrome/available?)
       (~getter js/chrome))))
