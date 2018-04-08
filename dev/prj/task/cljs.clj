(ns prj.task.cljs
  (:require [prj :refer [deftask]]
            [prj.cljs]))

(deftask npm-install
  [args]
  (prj.cljs/npm-install))
