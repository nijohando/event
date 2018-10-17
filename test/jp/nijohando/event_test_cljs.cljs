(ns jp.nijohando.event-test-cljs
  (:require [clojure.test :as t :refer [is deftest testing async]]
            [clojure.core.async :as ca :include-macros true]
            [jp.nijohando.ext.async :as xa :include-macros true]
            [jp.nijohando.event :as ev :include-macros true]
            [jp.nijohando.failable :as f :include-macros true]
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
            (is (f/succ? (xa/>! emitter event1 :timeout 1000)))
            (ev/close! bus)
            (is (f/succ? (xa/>! emitter event1 :timeout 1000)))
            (ca/<! (ca/timeout 1000))
            (let [x (xa/>! emitter event1 :timeout 1000)]
              (is (f/fail? x))
              (is (= ::xa/closed @x)))
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
            (is (f/succ? (xa/>! emitter event1 :timeout 1000)))
            (ca/<! (ca/timeout 100))
            (ev/close! bus)
            (let [x (xa/<! listener :timeout 1000)]
              (is (f/succ? x))
              (is (= :bar (:value x))))
            (let [x (xa/<! listener :timeout 1000)]
              (is (f/succ? x))
              (is (nil? x)))
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
            (is (f/succ? (xa/>! emitter event1 :timeout 1000)))
            (ca/<! (ca/timeout 100))
            (let [x (xa/>! emitter event1 :timeout 1000)]
              (is (f/fail? x))
              (is (= ::xa/closed @x)))
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
            (is (f/succ? (xa/>! emitter (ev/event "/bar" :bar) :timeout 1000)))
            (is (f/succ? (xa/>! emitter (ev/event "/foo" :foo) :timeout 1000)))
            (let [{:keys [path header value] :as x} (xa/<! listener :timeout 1000)
                  emitter-id (:emitter-id header)]
              (is (f/succ? x))
              (is (= "/foo" path))
              (is (= :foo value))
              (is (= 1 emitter-id)))
            (let [x (xa/<! listener :timeout 1000)]
              (is (f/fail? x))
              (is (= ::xa/timeout @x)))
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
            (is (f/succ? (xa/>! emitter (ev/event "/bar" :bar) :timeout 1000)))
            (is (f/succ? (xa/>! emitter (ev/event "/foo" :foo1) :timeout 1000)))
            (is (f/succ? (xa/>! emitter (ev/event "/foo" :foo2) :timeout 1000)))
            (let [{:keys [path header value] :as x} (xa/<! listener :timeout 1000)
                  emitter-id (:emitter-id header)]
              (is (f/succ? x))
              (is (= "/foo" path))
              (is (= :foo1 value))
              (is (= 1 emitter-id)))
            (let [{:keys [path header value] :as x} (xa/<! listener :timeout 1000)
                  emitter-id (:emitter-id header)]
              (is (f/succ? x))
              (is (= "/foo" path))
              (is (= :foo2 value))
              (is (= 1 emitter-id)))
            (let [x (xa/<! listener :timeout 1000)]
              (is (f/fail? x))
              (is (= ::xa/timeout @x)))
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
            (is (f/succ? (xa/>! emitter (ev/event "/bar" :bar) :timeout 1000)))
            (is (f/succ? (xa/>! emitter (ev/event "/foo/1" :foo) :timeout 1000)))
            (let [{:keys [path header value] :as x} (xa/<! listener :timeout 1000)
                  emitter-id (:emitter-id header)
                  {:keys [template path-params]}(:route header)]
              (is (f/succ? x))
              (is (= "/foo/1" path))
              (is (= :foo value))
              (is (= 1 emitter-id))
              (is (= "/foo/:id" template))
              (is (= {:id "1"} path-params)))
            (let [x (xa/<! listener :timeout 1000)]
              (is (f/fail? x))
              (is (= ::xa/timeout @x)))
            (done)
            (end)))))))

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
            (is (f/succ? (xa/>! emitter (ev/event "/bar" :bar) :timeout 1000)))
            (is (f/succ? (xa/>! emitter (ev/event "/foo/1" :foo1) :timeout 1000)))
            (is (f/succ? (xa/>! emitter (ev/event "/foo/2" :foo2) :timeout 1000)))
            (let [{:keys [path header value] :as x} (xa/<! listener)
                  emitter-id (:emitter-id header)
                  {:keys [template path-params]} (:route header)]
              (is (f/succ? x))
              (is (= "/foo/1" path))
              (is (= :foo1 value))
              (is (= 1 emitter-id))
              (is (= "/foo/:id" template))
              (is (= {:id "1"} path-params)))
            (let [{:keys [path header value] :as x} (xa/<! listener)
                  emitter-id (:emitter-id header)
                  {:keys [template path-params]} (:route header)]
              (is (f/succ? x))
              (is (= "/foo/2" path))
              (is (= :foo2 value))
              (is (= 1 emitter-id))
              (is (= "/foo/:id" template))
              (is (= {:id "2"} path-params)))
            (let [x (xa/<! listener :timeout 1000)]
              (is (f/fail? x))
              (is (= ::xa/timeout @x)))
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
            (is (f/succ? (xa/>! emitter (ev/event "/foo/1" :foo1) :timeout 1000)))
            (is (f/succ? (xa/>! emitter (ev/event "/bar/1" :bar1) :timeout 1000)))
            (let [{:keys [path header value] :as x} (xa/<! listener :timeout 1000)]
              (is (f/succ? x))
              (is (= "/foo/1" path))
              (is (= "/foo/:id" (get-in header [:route :template]))))
            (let [{:keys [path header value] :as x} (xa/<! listener :timeout 1000)]
              (is (f/succ? x))
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
            (is (f/succ? (xa/>! emitter1 (ev/event "/foo" "hello") :timeout 1000)))
            (let [x (xa/<! listener2 :timeout 1000)]
              (is (f/succ? x))
              (is (= "hello" (:value x)))
              (is (f/succ? (xa/>! emitter2 (ev/reply-to x "world!")))))
            (let [x (xa/<! reply1 :timeout 1000)]
              (is (f/succ? x))
              (is (= "world!" (:value x))))
            (done)
            (end)))))))
