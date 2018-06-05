(ns prj.task.test
  (:require [jp.nijohando.prj.core :refer [deftask]]
            [jp.nijohando.prj.test :as prj-test]))

(deftask test-clj
  [conf & test-case-symbols]
  (prj-test/run-tests 'jp.nijohando.event-test 'jp.nijohando.event-test-clj))
