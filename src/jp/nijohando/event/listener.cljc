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
  [from to]
  (ca/go-loop []
    (let [v (ca/<! from)]
      (if (nil? v)
        (ca/close! to)
        (if-not (true? (ca/>! to v))
          (ca/close! from)
          (recur)))))
  to)

(defn listen
  [routes listener-ch]
  (let [raw-routes (if (string? routes)
                     [routes]
                     routes)
        xform (comp (route raw-routes)
                    (routing-filter))
        filter-ch (ca/chan 1 xform)]
    (pipe filter-ch listener-ch)
    filter-ch))

