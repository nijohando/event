(ns prj.test.runner
  (:refer-clojure :exclude [test])
  (:require [cljs.test :as t]
            [prj.test.cases]))

(defn -main []
  (enable-console-print!)
  (prj.test.cases/run-all))

(defmethod t/report [:cljs.test/default :end-run-tests] [m]
  (when-not (t/successful? m)
    ((aget js/process "exit") 1)))

(set! *main-cli-fn* -main)
