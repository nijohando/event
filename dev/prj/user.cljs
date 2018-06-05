(ns prj.user
  (:require [jp.nijohando.event :as event :include-macros true]
            [jp.nijohando.event-test]
            [jp.nijohando.event-test-cljs]
            [cljs.test :refer-macros [run-tests]]))

(defn test-cljs
  []
  (run-tests 'jp.nijohando.event-test
             'jp.nijohando.event-test-cljs))
