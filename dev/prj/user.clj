(ns prj.user
  (:require [jp.nijohando.event :as f]
            [jp.nijohando.prj.test :as prj-test]
            [prj.cljs]
            [clojure.tools.namespace.repl :refer [refresh]]))

(defn test-clj
  []
  (prj-test/run-tests 'jp.nijohando.event-test 'jp.nijohando.event-test-clj))

(defn test-cljs
  []
  (prj.cljs/test-cljs))

(defn repl-cljs
  []
  (prj.cljs/repl-cljs))
