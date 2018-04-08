(ns prj.task.repl
  (:require [prj :refer [deftask]]
            [prj.repl]))

(deftask repl-clj
  [args]
  (prj.repl/repl-clj))

(deftask repl-cljs
  [args]
  (prj.repl/repl-cljs))
