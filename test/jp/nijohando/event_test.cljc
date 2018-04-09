(ns jp.nijohando.event-test
  (:require #?(:clj  [clojure.test :as t :refer [run-tests is are deftest testing]]
               :cljs [cljs.test :as t :refer-macros [run-tests is are deftest testing]])
            #?(:clj  [clojure.core.async :as ca]
               :cljs [cljs.core.async :as ca :include-macros true])
            [jp.nijohando.event :as ev :include-macros true]))

(deftest create-event
  (testing "Event can be created with path"
    (let [event (ev/event "/foo")]
      (is (map? event))
      (is (= "/foo" (:path event)))))
  (testing "Event can be created with path and value"
    (let [event (ev/event "/foo" :foo)]
      (is (map? event))
      (is (= "/foo" (:path event)))
      (is (= :foo (:value event))))))

(deftest create-reply-event
  (testing "Reply event can be created with an original event"
    (let [original-event {:path "/foo" :header {:emitter-id 7}} 
          reply-event (ev/reply-to original-event)]
      (is (map? reply-event))
      (is (= "/emitters/7/reply" (:path reply-event)))))
  (testing "Reply event can be created with path and "
    (let [original-event {:path "/foo" :header {:emitter-id 7}} 
          reply-event (ev/reply-to original-event :hello)]
      (is (map? reply-event))
      (is (= "/emitters/7/reply" (:path reply-event)))
      (is (= :hello (:value reply-event))))))

