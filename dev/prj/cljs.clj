(ns prj.cljs
  (:require [cljs.build.api]
            [meta-merge.core :refer [meta-merge]]
            [prj])
  (:import (java.lang ProcessBuilder$Redirect)))

(def configs
  (let [output-dir (str prj/work-dir "/out")
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
               :compiler     {:main          "prj.test.runner"
                              :optimizations :none
                              :source-map    true}}
     :prod {}}))

(defn config
  [conf-or-id]
  (if (keyword? conf-or-id)
    (->> configs
         ((juxt :default conf-or-id))
         (apply meta-merge)
         (#(assoc % :id conf-or-id)))
    conf-or-id))


(defn run-node
  [jsfile-path]
  (let [p (-> (ProcessBuilder. ["node", jsfile-path])
              (.redirectOutput ProcessBuilder$Redirect/INHERIT)
              (.redirectError ProcessBuilder$Redirect/INHERIT)
              (.start))]
    {:wait-for (delay (.waitFor p) (.exitValue p))
     :stop (delay (.destroy p))}))

(defn npm-install
  ([]
   (npm-install nil))
  ([conf-or-id]
   (let [conf (config (or conf-or-id :dev))
         npm-deps (get-in conf [:compiler :npm-deps])]
     (println "Install npm dependencies...") (cljs.build.api/install-node-deps! npm-deps conf)
     (println "Done."))))

(defn build-cljs
  ([]
   (build-cljs nil))
  ([conf-or-id]
   (let [conf (config (or conf-or-id :dev))
         sources (comp #(apply cljs.build.api/inputs %) :source-paths)]
     (->> conf
          ((juxt sources :compiler))
          (apply cljs.build.api/build)))))
