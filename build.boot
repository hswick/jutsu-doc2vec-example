(set-env!
  :resource-paths #{"src"}
  :dependencies '[[org.clojure/clojure "1.9.0-alpha15"]
                  [nightlight "1.6.5" :scope "test"]])

(require
  '[nightlight.boot :refer [nightlight]]
  'jutsu-doc2vec-example.core)

(deftask night []
  (comp
    (wait)
    (nightlight :port 4000)))

(deftask run []
  (with-pass-thru _
    (jutsu-doc2vec-example.core/-main)))
