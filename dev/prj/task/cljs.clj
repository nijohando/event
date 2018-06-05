(ns prj.task.cljs
  (:require [jp.nijohando.prj.core :refer [deftask]]
            [prj.cljs]))

(deftask npm-install
  [_]
  (prj.cljs/npm-install))

(deftask repl-cljs
  [_]
  (prj.cljs/repl-cljs))

(deftask test-cljs
  [_]
  (prj.cljs/test-cljs))

