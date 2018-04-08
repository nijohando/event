(ns jp.nijohando.event.protocols)

(defprotocol Emittable
  (emitize [this emitter-ch reply-ch]))

(defprotocol Listenable
  (listen [this routes listener-ch]))

(defprotocol Closable
  (close! [this]))

(defprotocol IDGen
  (next-id! [this]))
