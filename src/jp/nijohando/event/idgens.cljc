(ns jp.nijohando.event.idgens
  (:refer-clojure :exclude [sequence])
  (:require [jp.nijohando.event.protocols :as dp]))

(defn sequence []
  (let [id (atom 0)]
    (reify
      dp/IDGen
      (next-id! [this]
        (swap! id inc)))))
