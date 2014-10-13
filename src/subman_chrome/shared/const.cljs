(ns subman-chrome.shared.const)

(def result-limit 5)

(def subman-url "http://subman.io/")

(def search-url (str subman-url "api/bulk-search/"))

(def sources-url (str subman-url "api/list-sources/"))

(def repeat-timeout 1000)

(def options-sync-timeout 1000)

(def all-sources "all")

(def all-sources-id -1)

(def default-lang "english")

(def default-options {:language default-lang
                      :source all-sources})

(def default-sources {0 "addicted"
                      1 "podnapisi"
                      2 "opensubtitles"
                      3 "subscene"
                      4 "notabenoid"
                      5 "uksubtitles"})
