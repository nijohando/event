(ns prj.cljs
  (:require [jp.nijohando.prj.cljs :as prj-cljs]
            [jp.nijohando.prj.cljs.nodejs :as prj-cljs-nodejs]
            [jp.nijohando.prj.cljs.repl-figwheel :as prj-cljs-repl]
            [jp.nijohando.prj.cljs.test :as prj-cljs-test]
            [prj.config :refer [cljs-builds]]))

(def config (partial prj-cljs/merge-config cljs-builds))

(defn npm-install
  []
  (-> (config [:default :dev])
       (prj-cljs-nodejs/npm-install)))

(defn repl-cljs
  ([]
   (repl-cljs [:default :dev]))
  ([profiles]
   (-> (config profiles)
        (prj-cljs-repl/start-repl))))

(defn test-cljs
  ([]
   (test-cljs [:default :test]))
  ([profiles]
   (-> (config profiles)
       (prj-cljs-test/run-tests ['jp.nijohando.event-test
                                 'jp.nijohando.event-test-cljs]))))
