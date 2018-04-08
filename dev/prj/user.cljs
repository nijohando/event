(ns prj.user
  (:require [prj.test.cases]))

(defn test-cljs
  []
  (let [{:keys [fail error]} (prj.test.cases/run-all)]
    (and (zero? fail) (zero? error))))
