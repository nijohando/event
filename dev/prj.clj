(ns prj
  (:require [environ.core]))

(def ^:private tasks-sym (gensym))

(defn- keyword*
  [s]
  (-> (if (clojure.string/starts-with? s ":")
        (subs s 1)
        s)
      keyword))

(def env (fn [name]
           (-> environ.core/env
               (get name)
               not-empty
               (or (throw (ex-info (str "Environment variable " name " is not set") {}))))))

(def work-dir (env :work-dir))

(defmacro deftask
  [name args & body]
  (let [task-ns *ns*
        tasks @(or (ns-resolve task-ns tasks-sym) (intern *ns* tasks-sym (atom {})))
        task-sym (gensym)
        main-fn (fn [& [task-name & args]]
                  (-> (if-some [task (some->> task-name
                                              clojure.string/trim
                                              not-empty
                                              keyword*
                                              (get @tasks)
                                              (ns-resolve task-ns))]
                        (try
                          (let [result (task args)]
                            (cond
                              (number? result) result
                              (boolean? result) (if result 0 1)
                              :else 0))
                          (finally
                            (shutdown-agents)))
                        (do
                          (println "task not found")
                          1))
                      System/exit))]
    (swap! tasks assoc (keyword name) task-sym)
    (when-not (ns-resolve task-ns '-main)
      (intern task-ns '-main main-fn))
    `(defn ~task-sym
       [~@args]
       ~@body)))
