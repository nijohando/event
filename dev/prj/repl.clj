(ns prj.repl
  (:require [cljs.repl]
            [cider.nrepl]
            [figwheel-sidecar.repl-api :as ra]
            [prj]
            [prj.cljs]))

(defn- find-available-port
  []
  (let [s (java.net.ServerSocket. 0)]
    (.close s)
    (.getLocalPort s)))

(defn- run-figwheel
  [conf]
  (ra/start-figwheel!
   {:figwheel-options {:server-logfile (str prj/work-dir "/figwheel_server.log")}
    :build-ids        [(:id conf)]
    :all-builds       [(merge conf {:figwheel true})]}))

(defn repl-clj
  []
  (let [port (find-available-port)
        server (clojure.tools.nrepl.server/start-server :port port :handler cider.nrepl/cider-nrepl-handler)]
    (spit ".nrepl-port" port)
    ;(clojure.main/repl :init #(in-ns 'prj.user))
    (clojure.main/repl :init #(do (in-ns 'prj.user)
                                  (clojure.core/use 'clojure.core)
                                  (use 'prj.user)))
    (clojure.tools.nrepl.server/stop-server server)))

(defn repl-cljs
  ([]
   (repl-cljs nil))
  ([config-or-id]
   (let [conf (prj.cljs/config (or config-or-id :dev))
         jsfile-path (get-in conf [:compiler :output-to])]
     (run-figwheel conf)
     (let [p (prj.cljs/run-node jsfile-path)]
       (ra/cljs-repl)
       (ra/stop-figwheel!)
       @(:stop p)))))
