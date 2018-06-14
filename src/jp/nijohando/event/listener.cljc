(ns jp.nijohando.event.listener
  (:require [clojure.core.async :as ca]
            [reitit.core :as r]))

(defn- route
  [raw-routes]
  (let [router (r/router raw-routes)
        f (fn [event]
            (->> event
                 :path
                 (r/match-by-path router)
                 (assoc-in event [:header :route])))]
    (map f)))

(defn- routing-filter
  []
  (filter (comp some? #(get-in % [:header :route]))))

(defn- pipe
  [from to timeout]
  (ca/go-loop []
    (let [v (ca/<! from)]
      (if (nil? v)
        (ca/close! to)
        (let [timeout-ch (ca/timeout timeout)
              [v ch] (ca/alts! [[to v] timeout-ch])]
          (if (false? v)
            (ca/close! from)
            (recur)))))))

(defn listen
  ([routes listener-ch]
   (listen routes listener-ch nil))
  ([routes listener-ch {:keys [timeout]}]
   (let [timeout (or timeout 100)
         raw-routes (if (string? routes)
                      [routes]
                      routes)
         xform (comp (route raw-routes)
                     (routing-filter))
         filter-ch (ca/chan 1 xform)]
     (pipe filter-ch listener-ch timeout)
     filter-ch)))


