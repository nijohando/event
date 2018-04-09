(ns jp.nijohando.event.buses
  (:require [jp.nijohando.event.protocols :as ev]
            [jp.nijohando.event.idgens :as idgens]
            [jp.nijohando.event.listener :as listener]
            [jp.nijohando.event.emitter :as emitter]
            [clojure.core.async :as ca]))

(defn bus
  ([opts]
   (bus (ca/chan) opts))
  ([bus-ch {:keys [idgen] :as opts}]
  (let [mult-ch (ca/mult bus-ch)
        idgen (or idgen (idgens/sequence))]
    (reify
      ev/Emittable
      (emitize [this emitter-ch reply-ch]
        (let [emitter-id (ev/next-id! idgen)]
          (when reply-ch
            (ca/tap mult-ch (listener/listen [(str "/emitters/" emitter-id "/reply")] reply-ch)))
          (emitter/pipe emitter-id emitter-ch bus-ch)
          nil))
      ev/Listenable
      (listen [this routes listener-ch]
        (ca/tap mult-ch (listener/listen routes listener-ch))
        nil)
      ev/Closable
      (close! [this]
        (ca/close! bus-ch))))))

(defn- buffered-bus
  [buffer opts]
  (bus (ca/chan buffer) opts))

(defn blocking-bus
  [size opts]
  (buffered-bus (ca/buffer size) opts))

(defn sliding-bus
  [size opts]
  (buffered-bus (ca/sliding-buffer size) opts))

(defn dropping-bus
  [size opts]
  (buffered-bus (ca/dropping-buffer size) opts))


