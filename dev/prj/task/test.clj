(ns prj.task.test
  (:require [prj :refer [deftask]]
            [prj.test]))

(deftask test-clj
  [args]
  (prj.test/test-clj))

(deftask test-cljs
  [args]
  (prj.test/test-cljs))
