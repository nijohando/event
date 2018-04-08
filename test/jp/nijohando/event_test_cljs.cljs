(ns jp.nijohando.event-test-cljs
  (:require [cljs.test :as t :refer-macros [is deftest testing async]]
            [cljs.core.async :as ca :include-macros true]
            [jp.nijohando.event :as ev :include-macros true]
            [jp.nijohando.deferable :as d :include-macros true]))

(defn- emit!
  ([emitter-ch event]
   (emit! emitter-ch event nil))
  ([emitter-ch event timeout-ch]
   (ca/go
     (let [timeout-ch (or timeout-ch (ca/timeout 10))
           [v ch] (ca/alts! [[emitter-ch event] timeout-ch])]
       (cond
         (= ch emitter-ch) (if (true? v)
                             :ok
                             :close)
         (= ch timeout-ch) :timeout)))))

(defn- recv!
  ([listener-ch]
   (recv! listener-ch nil))
  ([listener-ch timeout-ch]
   (ca/go
     (let [timeout-ch (or timeout-ch (ca/timeout 10))
           [v ch] (ca/alts! [listener-ch timeout-ch])]
       (cond
         (= ch listener-ch) [:ok v]
         (= ch timeout-ch) [:timeout])))))

(deftest close-test1
  (testing "Emitter channel must be closed when the bus is closed"
    (async end
      (d/do** done
        (let [bus (ev/bus)
              _ (d/defer (ev/close! bus))
              emitter (ca/chan)
              _ (d/defer (ca/close! emitter))
              event1 (ev/event "/bar" :bar)]
          (ca/go
            (ev/emitize bus emitter)
            (is (= :ok (ca/<! (emit! emitter event1))))
            (ev/close! bus)
            (is (= :ok (ca/<! (emit! emitter event1))))
            (ca/<! (ca/timeout 100))
            (is (= :close (ca/<! (emit! emitter event1))))
            (done)
            (end)))))))

(deftest close-test2
  (testing "Listener channel must be closed when the bus is closed"
    (async end
      (d/do** done
        (let [bus (ev/bus)
              emitter (ca/chan)
              listener (ca/chan)
              event1 (ev/event "/bar" :bar)]
          (ca/go
            (ev/emitize bus emitter)
            (ev/listen bus "/bar" listener)
            (is (= :ok (ca/<! (emit! emitter event1))))
            (ca/<! (ca/timeout 100))
            (ev/close! bus)
            (let [[r v] (ca/<! (recv! listener))]
              (is (= :ok r))
              (is (= :bar (:value v))))
            (let [[r v] (ca/<! (recv! listener))]
              (is (= :ok r))
              (is (nil? v)))
            (done)
            (end)))))))

(deftest close-test3
  (testing "Emitting must not be blocked against the closed bus"
    (async end
      (d/do** done
        (let [bus (ev/bus)
              _ (d/defer (ev/close! bus))
              emitter (ca/chan)
              _ (d/defer (ca/close! emitter))
              event1 (ev/event "/bar" :bar)]
          (ca/go
            (ev/emitize bus emitter)
            (ev/close! bus)
            (is (= :ok (ca/<! (emit! emitter event1))))
            (ca/<! (ca/timeout 100))
            (dotimes [n 1]
              (is (= :close (ca/<! (emit! emitter event1)))))
            (done)
            (end)))))))

(deftest emit-and-listen-test1
  (testing "Listener can receive an event matching fixed path"
    (async end
      (d/do** done
        (let [bus (ev/bus)
              _ (d/defer (ev/close! bus))
              emitter (ca/chan)
              _ (d/defer (ca/close! emitter))
              listener (ca/chan)
              _ (d/defer (ca/close! listener))]
          (ca/go
            (ev/emitize bus emitter)
            (ev/listen bus "/foo" listener)
            (is (= :ok (ca/<! (emit! emitter (ev/event "/bar" :bar)))))
            (is (= :ok (ca/<! (emit! emitter (ev/event "/foo" :foo)))))
            (let [[r {:keys [path header value]}] (ca/<! (recv! listener))
                  emitter-id (:emitter-id header)]
              (is (= :ok r))
              (is (= "/foo" path))
              (is (= :foo value))
              (is (= 1 emitter-id))
              (let [[r _] (ca/<! (recv! listener))]
                (is (= r :timeout))))
            (done)
            (end)))))))


(deftest emit-and-listen-test2
  (testing "Listener can receive events matching fixed path"
    (async end
      (d/do** done
        (let [bus (ev/bus)
              _ (d/defer (ev/close! bus))
              emitter (ca/chan)
              _ (d/defer (ca/close! emitter))
              listener (ca/chan)
              _ (d/defer (ca/close! listener))]
          (ca/go
            (ev/emitize bus emitter)
            (ev/listen bus "/foo" listener)
            (is (= :ok (ca/<! (emit! emitter (ev/event "/bar" :bar)))))
            (is (= :ok (ca/<! (emit! emitter (ev/event "/foo" :foo1)))))
            (is (= :ok (ca/<! (emit! emitter (ev/event "/foo" :foo2)))))
            (let [[r {:keys [path header value]}] (ca/<! (recv! listener))
                  emitter-id (:emitter-id header)]
              (is (= :ok r))
              (is (= "/foo" path))
              (is (= :foo1 value))
              (is (= 1 emitter-id)))
            (let [[r {:keys [path header value]}] (ca/<! (recv! listener))
                  emitter-id (:emitter-id header)]
              (is (= :ok r))
              (is (= "/foo" path))
              (is (= :foo2 value))
              (is (= 1 emitter-id)))
            (let [[r _] (ca/<! (recv! listener))]
              (is (= r :timeout)))
            (done)
            (end)))))))

(deftest emit-and-listen-test3
  (testing "Listener can receive an event matching parameterized path"
    (async end
      (d/do** done
        (let [bus (ev/bus)
              emitter (ca/chan)
              listener (ca/chan)]
          (ca/go
            (ev/emitize bus emitter)
            (ev/listen bus "/foo/:id" listener)
            (is (= :ok (ca/<! (emit! emitter (ev/event "/bar" :bar)))))
            (is (= :ok (ca/<! (emit! emitter (ev/event "/foo/1" :foo)))))
            (let [[r {:keys [path header value]}] (ca/<! (recv! listener))
                  emitter-id (:emitter-id header)
                  {:keys [template path-params]}(:route header)]
              (is (= :ok r))
              (is (= "/foo/1" path))
              (is (= :foo value))
              (is (= 1 emitter-id))
              (is (= "/foo/:id" template))
              (is (= {:id "1"} path-params)))
            (let [[r _] (ca/<! (recv! listener))]
              (is (= r :timeout)))
            (end)
            (done)))))))


(deftest emit-and-listen-test4
  (testing "Listener can receive events matching parameterized path"
    (async end
      (d/do** done
        (let [bus (ev/bus)
              emitter (ca/chan)
              listener (ca/chan)]
          (ca/go
            (ev/emitize bus emitter)
            (ev/listen bus "/foo/:id" listener)
            (is (= :ok (ca/<! (emit! emitter (ev/event "/bar" :bar)))))
            (is (= :ok (ca/<! (emit! emitter (ev/event "/foo/1" :foo1)))))
            (is (= :ok (ca/<! (emit! emitter (ev/event "/foo/2" :foo2)))))
            (let [[r {:keys [path header value]}] (ca/<! (recv! listener))
                  emitter-id (:emitter-id header)
                  {:keys [template path-params]} (:route header)]
              (is (= :ok r))
              (is (= "/foo/1" path))
              (is (= :foo1 value))
              (is (= 1 emitter-id))
              (is (= "/foo/:id" template))
              (is (= {:id "1"} path-params)))
            (let [[r {:keys [path header value]}] (ca/<! (recv! listener))
                  emitter-id (:emitter-id header)
                  {:keys [template path-params]}(:route header)]
              (is (= :ok r))
              (is (= "/foo/2" path))
              (is (= :foo2 value))
              (is (= 1 emitter-id))
              (is (= "/foo/:id" template))
              (is (= {:id "2"} path-params)))
            (let [[r _] (ca/<! (recv! listener))]
              (is (= r :timeout)))
            (done)
            (end)))))))

(deftest emit-and-listen-test5
  (testing "Listener can receive events matching multiple pathes"
    (async end
      (d/do** done
        (let [bus (ev/bus)
              emitter (ca/chan)
              listener (ca/chan)]
          (ca/go
            (ev/emitize bus emitter)
            (ev/listen bus [["/foo/:id"]
                            ["/bar/:id"]] listener)
            (is (= :ok (ca/<! (emit! emitter (ev/event "/foo/1" :foo1)))))
            (is (= :ok (ca/<! (emit! emitter (ev/event "/bar/1" :bar1)))))
            (let [[r {:keys [path header value]}] (ca/<! (recv! listener))]
              (is (= :ok r))
              (is (= "/foo/1" path))
              (is (= "/foo/:id" (get-in header [:route :template]))))
            (let [[r {:keys [path header value]}] (ca/<! (recv! listener))]
              (is (= :ok r))
              (is (= "/bar/1" path))
              (is (= "/bar/:id" (get-in header [:route :template]))))
            (done)
            (end)))))))

(deftest reply-test1
  (testing "Listener can reply to the emitter"
    (async end
      (d/do** done
        (let [bus (ev/bus)
              emitter1 (ca/chan)
              reply1 (ca/chan)
              emitter2 (ca/chan)
              listener2 (ca/chan)]
          (ca/go
            (ev/emitize bus emitter1 reply1)
            (ev/emitize bus emitter2)
            (ev/listen bus "/foo" listener2)
            (is (= :ok (ca/<! (emit! emitter1 (ev/event "/foo" "hello")))))
            (let [[r v] (ca/<! (recv! listener2))]
              (is (= :ok r))
              (is (= "hello" (:value v)))
              (is (= :ok (ca/<! (emit! emitter2 (ev/reply-to v "world!"))))))
            (let [[r v] (ca/<! (recv! reply1))]
              (is (= :ok r))
              (is (= "world!" (:value v))))
            (done)
            (end)))))))
