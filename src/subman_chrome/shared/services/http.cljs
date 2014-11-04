(ns subman-chrome.shared.services.http
  (:refer-clojure :exclude [get])
  (:require-macros [clj-di.core :refer [defprotocol*]])
  (:require [cljs-http.client :as http]))

(defprotocol* http-client
  (get [_ url])
  (post [_ url data]))

(deftype http-client-impl []
  http-client
  (get [_ url] (http/get url))
  (post [_ url data] (http/post url data)))
