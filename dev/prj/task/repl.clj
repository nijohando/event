(ns prj.task.repl
  (:require [jp.nijohando.prj.core :refer [deftask]]
            [jp.nijohando.prj.repl :as prj-repl]))

(deftask repl-clj
  [args]
  (prj-repl/start-nrepl-server)
  (prj-repl/start-repl 'prj.user)
  (prj-repl/stop-nrepl-server))

