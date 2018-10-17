(ns jp.nijohando.event-test-clj
  (:require [clojure.test :as t :refer [run-tests is are deftest testing]]
            [clojure.core.async :as ca]
            [jp.nijohando.ext.async :as xa]
            [jp.nijohando.event :as ev]
            [jp.nijohando.failable :as f]
            [jp.nijohando.deferable :as d]))

(deftest close-test

  (testing "Emitter channel must be closed when the bus is closed"
    (d/do*
      (let [bus (ev/bus)
            _ (d/defer (ev/close! bus))
            emitter (ca/chan)
            _ (d/defer (ca/close! emitter))
            event1 (ev/event "/bar" :bar)]
        (ev/emitize bus emitter)
        (is (f/succ? (xa/>!! emitter event1 :timeout 1000)))
        (ev/close! bus)
        (is (f/succ? (xa/>!! emitter event1 :timeout 1000)))
        (xa/<!! (ca/timeout 100))
        (let [x (xa/>!! emitter event1 :timeout 1000)]
          (is (f/fail? x))
          (is (= ::xa/closed @x))))))

  (testing "Listener channel must be closed when the bus is closed"
    (d/do*
      (let [bus (ev/bus)
            _ (d/defer (ev/close! bus))
            emitter (ca/chan)
            _ (d/defer (ca/close! emitter))
            listener (ca/chan)
            event1 (ev/event "/bar" :bar)]
        (ev/emitize bus emitter)
        (ev/listen bus "/bar" listener)
        (is (f/succ? (xa/>!! emitter event1 :timeout 1000)))
        (ca/<!! (ca/timeout 100))
        (ev/close! bus)
        (let [x (xa/<!! listener :timeout 1000)]
          (is (f/succ? x))
          (is (= :bar (:value x))))
        (let [x (xa/<!! listener :timeout 1000)]
          (is (f/succ? x))
          (is (nil? x))))))

  (testing "Emitting must not be blocked against the closed bus"
    (d/do*
      (let [bus (ev/bus)
            _ (d/defer (ev/close! bus))
            emitter (ca/chan)
            _ (d/defer (ca/close! emitter))
            event1 (ev/event "/bar" :bar)]
        (ev/emitize bus emitter)
        (ev/close! bus)
        ;; First emitting after the bus is closed can not recognize whether the bus is closed or not
        (is (f/succ? (xa/>!! emitter event1 :timeout 1000)))
        (ca/<!! (ca/timeout 100))
        (let [x (xa/>!! emitter event1 :timeout 1000)]
          (is (f/fail? x))
          (is (= ::xa/closed @x)))))))

(deftest emit-and-listen-test

  (testing "Listener can receive an event matching fixed path"
    (d/do*
     (let [bus (ev/bus)
           _ (d/defer (ev/close! bus))
           emitter (ca/chan)
           _ (d/defer (ca/close! emitter))
           listener (ca/chan)
           _ (d/defer (ca/close! listener))]
       (ev/emitize bus emitter)
       (ev/listen bus "/foo" listener)
       (is (f/succ? (xa/>!! emitter (ev/event "/bar" :bar) :timeout 1000)))
       (is (f/succ? (xa/>!! emitter (ev/event "/foo" :foo) :tineout 1000)))
       (let [{:keys [path header value] :as x} (xa/<!! listener :timeout 1000)
             emitter-id (:emitter-id header)]
         (is (f/succ? x))
         (is (= "/foo" path))
         (is (= :foo value))
         (is (= 1 emitter-id)))
       (let [x (xa/<!! listener :timeout 1000)]
         (is (f/fail? x))
         (is (= ::xa/timeout @x))))))

  (testing "Listener can receive events matching fixed path"
    (d/do*
     (let [bus (ev/bus)
           _ (d/defer (ev/close! bus))
           emitter (ca/chan)
           _ (d/defer (ca/close! emitter))
           listener (ca/chan)
           _ (d/defer (ca/close! listener))]
       (ev/emitize bus emitter)
       (ev/listen bus "/foo" listener)
       (is (f/succ? (xa/>!! emitter (ev/event "/bar" :bar) :timeout 1000)))
       (is (f/succ? (xa/>!! emitter (ev/event "/foo" :foo1) :timeout 1000)))
       (is (f/succ? (xa/>!! emitter (ev/event "/foo" :foo2) :timeout 1000)))
       (let [{:keys [path header value] :as x} (xa/<!! listener :timeout 1000)
             emitter-id (:emitter-id header)]
         (is (f/succ? x))
         (is (= "/foo" path))
         (is (= :foo1 value))
         (is (= 1 emitter-id)))
       (let [{:keys [path header value] :as x} (xa/<!! listener :timeout 1000)
             emitter-id (:emitter-id header)]
         (is (f/succ? x))
         (is (= "/foo" path))
         (is (= :foo2 value))
         (is (= 1 emitter-id)))
       (let [x (xa/<!! listener :timeout 1000)]
         (is (f/fail? x))
         (is (= ::xa/timeout @x))))))

  (testing "Listener can receive an event matching parameterized path"
    (d/do*
     (let [bus (ev/bus)
           _ (d/defer (ev/close! bus))
           emitter (ca/chan)
           _ (d/defer (ca/close! emitter))
           listener (ca/chan)
           _ (d/defer (ca/close! listener))]
       (ev/emitize bus emitter)
       (ev/listen bus "/foo/:id" listener)
       (is (f/succ? (xa/>!! emitter (ev/event "/bar" :bar) :timeout 1000)))
       (is (f/succ? (xa/>!! emitter (ev/event "/foo/1" :foo) :timeout 1000)))
       (let [{:keys [path header value] :as x} (xa/<!! listener :timeout 1000)
             emitter-id (:emitter-id header)
             {:keys [template path-params]} (:route header)]
         (is (f/succ? x))
         (is (= "/foo/1" path))
         (is (= :foo value))
         (is (= 1 emitter-id ))
         (is (= "/foo/:id" template))
         (is (= {:id "1"} path-params)))
       (let [x (xa/<!! listener :timeout 1000)]
         (is (f/fail? x))
         (is (= ::xa/timeout @x))))))

  (testing "Listener can receive events matching parameterized path"
    (d/do*
     (let [bus (ev/bus)
           _ (d/defer (ev/close! bus))
           emitter (ca/chan)
           _ (d/defer (ca/close! emitter))
           listener (ca/chan)
           _ (d/defer (ca/close! listener))]
       (ev/emitize bus emitter)
       (ev/listen bus "/foo/:id" listener)
       (is (f/succ? (xa/>!! emitter (ev/event "/bar" :bar) :timeout 1000)))
       (is (f/succ? (xa/>!! emitter (ev/event "/foo/1" :foo1) :timeout 1000)))
       (is (f/succ? (xa/>!! emitter (ev/event "/foo/2" :foo2) :timeout 1000)))
       (let [{:keys [path header value] :as x} (xa/<!! listener :timeout 1000)
             emitter-id (:emitter-id header)
             {:keys [template path-params]} (:route header)]
         (is (f/succ? x))
         (is (= "/foo/1" path))
         (is (= :foo1 value))
         (is (= 1 emitter-id))
         (is (= "/foo/:id" template))
         (is (= {:id "1"} path-params)))
       (let [{:keys [path header value] :as x} (xa/<!! listener :timeout 1000)
             emitter-id (:emitter-id header)
             {:keys [template path-params]} (:route header)]
         (is (f/succ? x))
         (is (= "/foo/2" path))
         (is (= :foo2 value))
         (is (= 1 emitter-id))
         (is (= "/foo/:id" template))
         (is (= {:id "2"} path-params)))
       (let [x (xa/<!! listener :timeout 1000)]
         (is (f/fail? x))
         (is (= ::xa/timeout @x))))))

  (testing "Listener can receive events matching multiple pathes"
    (d/do*
     (let [bus (ev/bus)
           _ (d/defer (ev/close! bus))
           emitter (ca/chan)
           _ (d/defer (ca/close! emitter))
           listener (ca/chan)
           _ (d/defer (ca/close! listener))]
       (ev/emitize bus emitter)
       (ev/listen bus [["/foo/:id"]
                       ["/bar/:id"]] listener)
       (is (f/succ? (xa/>!! emitter (ev/event "/foo/1" :foo1) :timeout 1000)))
       (is (f/succ? (xa/>!! emitter (ev/event "/bar/1" :bar1) :timeout 1000)))
       (let [x (xa/<!! listener :timeout 1000)]
         (is (f/succ? x))
         (is (= "/foo/1" (:path x)))
         (is (= "/foo/:id" (get-in x [:header :route :template]))))
       (let [x (xa/<!! listener :timeout 1000)]
         (is (f/succ? x))
         (is (= "/bar/1" (:path x)))
         (is (= "/bar/:id" (get-in x [:header :route :template]))))))))

(deftest reply-test
  (testing "Listener can reply to the emitter"
    (d/do*
     (let [bus (ev/bus)
           _ (d/defer (ev/close! bus))
           emitter1 (ca/chan)
           _ (d/defer (ca/close! emitter1))
           reply1 (ca/chan)
           _ (d/defer (ca/close! reply1))
           emitter2 (ca/chan)
           _ (d/defer (ca/close! emitter2))
           listener2 (ca/chan)
           _ (d/defer (ca/close! listener2))]
       (ev/emitize bus emitter1 reply1)
       (ev/emitize bus emitter2)
       (ev/listen bus "/foo" listener2)
       (is (f/succ? (xa/>!! emitter1 (ev/event "/foo" "hello") :timeout 1000)))
       (let [x (xa/<!! listener2 :timeout 1000)]
         (is (f/succ? x))
         (is (= "hello" (:value x)))
         (is (f/succ? (xa/>!! emitter2 (ev/reply-to x "world!") :timeout 1000))))
       (let [x (xa/<!! reply1 :timeout 1000)]
         (is (f/succ? x))
         (is (= "world!" (:value x))))))))
