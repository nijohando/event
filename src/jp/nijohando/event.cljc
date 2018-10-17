(ns jp.nijohando.event
  (:require [jp.nijohando.event.protocols :as ev]
            [jp.nijohando.event.buses :as buses]))

(defn event
  ([path]
   (event path nil))
  ([path value]
   (merge {:path path}
          (when (some? value)
            {:value value}))))

(defn reply-to
  ([event]
   (reply-to event nil))
  ([event value]
   (merge {:path (str "/emitters/" (get-in event [:header :emitter-id]) "/reply")}
          (when (some? value)
            {:value value}))))

(defn emitize
  ([bus emitter-ch]
   (emitize bus emitter-ch nil))
  ([bus emitter-ch reply-ch]
   (ev/emitize bus emitter-ch reply-ch)))

(defn listen
  [bus routes listener-ch]
  (ev/listen bus routes listener-ch))

(defn close! [& buses]
  (doseq [bus buses]
    (ev/close! bus)))

(defn bus
  ([]
   (bus nil))
  ([opts]
   (buses/bus opts)))

(defn blocking-bus
  ([size]
   (blocking-bus size nil))
  ([size opts]
   (buses/blocking-bus size opts)))

(defn sliding-bus
  ([size]
   (sliding-bus size nil))
  ([size opts]
   (buses/sliding-bus size opts)))

(defn dropping-bus
  ([size]
   (dropping-bus size nil))
  ([size opts]
   (buses/dropping-bus size opts)))
