(ns jp.nijohando.event.emitter
  (:require [clojure.core.async :as ca]))

(defn pipe
  [emitter-id emitter-ch bus-ch]
  (ca/go-loop []
    (when-some [v (ca/<! emitter-ch)]
      (when-let [event (when (map? v)
                         (assoc-in v [:header :emitter-id] emitter-id))]
        (when-not (ca/>! bus-ch event)
          (ca/close! emitter-ch)))
      (recur))))
