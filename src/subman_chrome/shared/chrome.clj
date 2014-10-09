(ns subman-chrome.shared.chrome)

(defmacro defattr
  [attr getter]
  `(def ~attr
     (when (subman-chrome.shared.chrome/available?)
       (aget js/chrome (name '~getter)))))
