(set-env!
  :resource-paths #{"src" "data"}
  :dependencies '[[org.clojure/clojure "1.8.0"]
                  [org.nd4j/nd4j-native-platform "0.8.0"]
                  [hswick/jutsu "0.0.4"]
                  [nightlight "1.7.1" :scope "test"]
                  [hswick/jutsu.nlp "0.0.2"]
                  [hswick/jutsu.matrix "0.0.4"]
                  [cheshire "5.7.1"]])

(task-options!
       pom {:project 'news-articles-viz
            :version "0.1.0"}
       jar {:main 'news-articles.core
            :manifest {"Description" "totally awesomeness"}})

(require
  '[nightlight.boot :refer [nightlight]])

(deftask night []
  (comp
    (wait)
    (nightlight :port 4000)))

(deftask build []
  (comp
    (pom)
    (jar)
    (target)))

(require 'news-articles.core)

(deftask run []
  (with-pass-thru _
    (news-articles.core/-main)))
