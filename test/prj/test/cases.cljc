(ns prj.test.cases
  (:require #?(:clj  [clojure.test :as t :refer  [run-tests]]
               :cljs [cljs.test :as t :refer-macros [run-tests]])
            [jp.nijohando.event-test]
            #?(:clj [jp.nijohando.event-test-clj]
               :cljs [jp.nijohando.event-test-cljs])))

(defn run-all
  []
  (run-tests #?@(:clj  ['jp.nijohando.event-test 'jp.nijohando.event-test-clj]
                 :cljs ['jp.nijohando.event-test 'jp.nijohando.event-test-cljs])))

