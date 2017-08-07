(ns news-articles.core
  (:require [jutsu.nlp.core :as nlp]
            [jutsu.nlp.sentence-iterator :as iter]
            [jutsu.nlp.util :as util]
            [jutsu.nlp.tokenization :as token]
            [jutsu.matrix.core :as m]
            [cheshire.core :as json]))

;;Only need to run this once to create your word-2-vec model
;;Then the code can read from the saved model
(defn train-and-save-w2v []
  (let [w2v (nlp/word-2-vec
              (iter/dir-iterator "articles")
              (token/default-tokenizer-factory)
              {:min-word-frequency 6
               :window-size 10
               :layer-size 150
               :stopwords (nlp/stop-words)})]
    (nlp/fit! w2v)
    (nlp/write-word-vectors w2v "word_vectors.csv")))

;;Turn the files into vectors
(defn files-2-vecs [w2v dir-path]
  (let [fs (rest (file-seq (clojure.java.io/file dir-path)))]
    (map (fn [f] (nlp/doc-2-vec w2v (slurp f))) fs)))

;;Split up vectors by category
(defn doc-vec-map [w2v base-path categories]
  (into {}
    (map (fn [category]
           [category (files-2-vecs w2v (str base-path category))])
      categories)))

;;Put document vectors into one big array and run Principal Component Analysis on it
;;to compress the vectors to 2 dimensions.
(defn compress-vecs [vecs]
  (->> (map #(apply m/vstack %) (vals vecs))
       (apply m/vstack)
       (m/normalize-zero!)
       (m/pca 2)))

;;Split vectors back up into 5 categories
(defn compressed-categories [compressed-vecs file-counts categories]
  (loop [categories categories i 0 begin 0 new-map {}]
    (if (empty? categories) new-map
      (recur 
        (rest categories) 
        (inc i) 
        (+ begin (nth file-counts i))
        (assoc new-map 
          (first categories) 
          (for [n (range begin (+ begin (nth file-counts i)))]
            (m/get-row compressed-vecs n)))))))

;;Generate data in format to be graphed by jutsu
(defn gen-graph-data [category-vecs-map]
  (let [default-map {:mode "markers"
                     :type "scatter"}]
    (seq (map
           (fn [[category vecs]]
             (merge default-map
               {:x (map #(m/get-double % 0) vecs)
                :y (map #(m/get-double % 1) vecs)
                :name category}))
           category-vecs-map))))

(defn write-graph-data [graph-data filename]
  (spit filename (json/generate-string graph-data)))

(defn -main []
  ;(train-and-save w2v)
  (let [w2v (nlp/read-word-vectors (clojure.java.io/file "word_vectors.csv"))
        base-path "data/articles/bbc/"
        categories ["business" "entertainment" "politics" "sport" "tech"]
        news-articles-vecs (doc-vec-map w2v base-path categories)
        file-counts (map count (vals news-articles-vecs))]
    (-> (compress-vecs news-articles-vecs)
        (compressed-categories file-counts categories)
        (gen-graph-data)
        (write-graph-data "news-articles-graph-data.json"))))
