(ns prj.config
  (:require [jp.nijohando.prj.core :refer [work-dir]]))

(def cljs-builds
  (let [output-dir (str work-dir "/out")
        output-to  (str output-dir "/index.js")]
    {:default {:source-paths ["src"]
               :compiler     {:main          "jp.nijohando.event"
                              :output-dir    output-dir
                              :output-to     output-to
                              :source-map    (str output-to ".map")
                              :npm-deps      {}
                              :install-deps  false
                              :target        :nodejs
                              :optimizations :simple
                              :verbose       true}}
     :dev     {:source-paths ["dev" "test"]
               :compiler     {:npm-deps      {:ws "4.0.0"}
                              :optimizations :none
                              :source-map    true}}
     :test    {:source-paths ["test"]
               :compiler     {:main          "jp.nijohando.prj.cljs.test.runner"
                              :optimizations :none
                              :source-map    true}}
     :prod {}}))

