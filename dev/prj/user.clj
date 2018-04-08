(ns prj.user
  (:require [prj.test :refer [test-clj test-cljs]]
            [prj.repl :refer [repl-cljs]]
            [prj.package :refer [update-pom deploy]]
            [prj.cljs :refer [npm-install build-cljs]]
            [jp.nijohando.event :as ev]
            [clojure.core.async :as ca]
            [clojure.tools.namespace.repl :refer [refresh]]))
