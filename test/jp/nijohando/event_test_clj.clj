(ns jp.nijohando.event-test-clj
  (:require [clojure.test :as t :refer [run-tests is are deftest testing]]
            [clojure.core.async :as ca]
            [jp.nijohando.event :as ev]
            [jp.nijohando.deferable :as d]))

(defn- emit!!
  ([emitter-ch event]
   (emit!! emitter-ch event nil))
  ([emitter-ch event timeout-ch]
   (let [timeout-ch (or timeout-ch (ca/timeout 500))
         [v ch] (ca/alts!! [[emitter-ch event] timeout-ch])]
     (cond
       (= ch emitter-ch) (if (true? v)
                           :ok
                           :close)
       (= ch timeout-ch) :timeout))))

(defn- recv!!
  ([listener-ch]
   (recv!! listener-ch nil))
  ([listener-ch timeout-ch]
   (let [timeout-ch (or timeout-ch (ca/timeout 10))
         [v ch] (ca/alts!! [listener-ch timeout-ch])]
     (cond
       (= ch listener-ch) [:ok v]
       (= ch timeout-ch) [:timeout]))))

(deftest close-test

  (testing "Emitter channel must be closed when the bus is closed"
    (d/do*
      (let [bus (ev/bus)
            _ (d/defer (ev/close! bus))
            emitter (ca/chan)
            _ (d/defer (ca/close! emitter))
            event1 (ev/event "/bar" :bar)]
        (ev/emitize bus emitter)
        (is (= :ok (emit!! emitter event1)))
        (ev/close! bus)
        (is (= :ok (emit!! emitter event1)))
        (is (= :close (emit!! emitter event1))))))

  (testing "Listener channel must be closed when the bus is closed"
    (d/do*
      (let [bus (ev/bus)
            _ (d/defer (ev/close! bus))
            emitter (ca/chan)
            _ (d/defer (ca/close! emitter))
            listener (ca/chan 1)
            event1 (ev/event "/bar" :bar)]
        (ev/emitize bus emitter)
        (ev/listen bus "/bar" listener)
        (is (= :ok (emit!! emitter event1)))
        (ev/close! bus)
        (let [[r v] (recv!! listener)]
          (is (= :ok r))
          (is (= :bar (:value v))))
        (let [[r v] (recv!! listener)]
          (is (= :ok r))
          (is (nil? v))))))

  (testing "Emitting must not be blocked against the closed bus"
    (d/do*
      (let [bus (ev/bus)
            _ (d/defer (ev/close! bus))
            emitter (ca/chan)
            _ (d/defer (ca/close! emitter))
            event1 (ev/event "/bar" :bar)]
        (ev/emitize bus emitter)
        (ev/close! bus)
        (is (= :ok (emit!! emitter event1)))
        (dotimes [n 1]
          (is (= :close (emit!! emitter event1))))))))

(deftest emit-and-listen-test

  (testing "Emitting must not be blocked even if no listeners"
    (d/do*
      (let [bus (ev/bus)
            _ (d/defer (ev/close! bus))
            emitter (ca/chan)
            _ (d/defer (ca/close! emitter))
            event-paths (atom #{})]
        (ev/emitize bus emitter)
        (doseq [x (range 1000)
                :let [epath (str "/bar" x) ]]
          (is (= :ok (emit!! emitter (ev/event epath :bar))))
          (swap! event-paths conj epath)))))

  (testing "Emitting must not be blocked even if there is a overflowed listener"
    (d/do*
      (let [bus (ev/bus)
            _ (d/defer (ev/close! bus))
            emitter (ca/chan)
            _ (d/defer (ca/close! emitter))
            listener (ca/chan)
            _ (d/defer (ca/close! listener))
            event-paths (atom #{})]
        (ev/emitize bus emitter)
        (ev/listen bus "/*" listener)
        (doseq [x (range 10)
                :let [epath (str "/bar" x) ]]
          (is (= :ok (emit!! emitter (ev/event epath :bar))))
          (swap! event-paths conj epath)))))

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
       (is (= :ok (emit!! emitter (ev/event "/bar" :bar))))
       (is (= :ok (emit!! emitter (ev/event "/foo" :foo))))
       (let [[r {:keys [path header value]}] (recv!! listener)
             emitter-id (:emitter-id header)]
         (is (= :ok r))
         (is (= "/foo" path))
         (is (= :foo value))
         (is (= 1 emitter-id))
         (let [[r _] (recv!! listener)]
           (is (= r :timeout)))))))

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
       (is (= :ok (emit!! emitter (ev/event "/bar" :bar))))
       (is (= :ok (emit!! emitter (ev/event "/foo" :foo1))))
       (is (= :ok (emit!! emitter (ev/event "/foo" :foo2))))
       (let [[r {:keys [path header value]}] (recv!! listener)
             emitter-id (:emitter-id header)]
         (is (= :ok r))
         (is (= "/foo" path))
         (is (= :foo1 value))
         (is (= 1 emitter-id)))
       (let [[r {:keys [path header value]}] (recv!! listener)
             emitter-id (:emitter-id header)]
         (is (= :ok r))
         (is (= "/foo" path))
         (is (= :foo2 value))
         (is (= 1 emitter-id)))
       (let [[r _] (recv!! listener)]
         (is (= r :timeout))))))

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
       (is (= :ok (emit!! emitter (ev/event "/bar" :bar))))
       (is (= :ok (emit!! emitter (ev/event "/foo/1" :foo))))
       (let [[r {:keys [path header value]}] (recv!! listener)
             emitter-id (:emitter-id header)
             {:keys [template path-params]} (:route header)]
         (is (= :ok r))
         (is (= "/foo/1" path))
         (is (= :foo value))
         (is (= 1 emitter-id ))
         (is (= "/foo/:id" template))
         (is (= {:id "1"} path-params)))
       (let [[r _] (recv!! listener)]
         (is (= r :timeout))))))

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
       (is (= :ok (emit!! emitter (ev/event "/bar" :bar))))
       (is (= :ok (emit!! emitter (ev/event "/foo/1" :foo1))))
       (is (= :ok (emit!! emitter (ev/event "/foo/2" :foo2))))
       (let [[r {:keys [path header value]}] (recv!! listener)
             emitter-id (:emitter-id header)
             {:keys [template path-params]} (:route header)]
         (is (= :ok r))
         (is (= "/foo/1" path))
         (is (= :foo1 value))
         (is (= 1 emitter-id))
         (is (= "/foo/:id" template))
         (is (= {:id "1"} path-params)))
       (let [[r {:keys [path header value]}] (recv!! listener)
             emitter-id (:emitter-id header)
             {:keys [template path-params]} (:route header)]
         (is (= :ok r))
         (is (= "/foo/2" path))
         (is (= :foo2 value))
         (is (= 1 emitter-id))
         (is (= "/foo/:id" template))
         (is (= {:id "2"} path-params)))
       (let [[r _] (recv!! listener)]
         (is (= r :timeout))))))

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
       (is (= :ok (emit!! emitter (ev/event "/foo/1" :foo1))))
       (is (= :ok (emit!! emitter (ev/event "/bar/1" :bar1))))
       (let [[r v] (recv!! listener)]
         (is (= :ok r))
         (is (= "/foo/1" (:path v)))
         (is (= "/foo/:id" (get-in v [:header :route :template]))))
       (let [[r v] (recv!! listener)]
         (is (= :ok r))
         (is (= "/bar/1" (:path v)))
         (is (= "/bar/:id" (get-in v [:header :route :template]))))))))

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
       (is (= :ok (emit!! emitter1 (ev/event "/foo" "hello"))))
       (let [[r v] (recv!! listener2)]
         (is (= :ok r))
         (is (= "hello" (:value v)))
         (is (= :ok (emit!! emitter2 (ev/reply-to v "world!")))))
       (let [[r v] (recv!! reply1)]
         (is (= :ok r))
         (is (= "world!" (:value v))))))))
