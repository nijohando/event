(ns prj.test
  (:require [prj.cljs]))

(defn test-clj
  []
  (require 'prj.test.cases)
  (let [{:keys [fail error]} ((resolve (symbol "prj.test.cases/run-all")))]
    (and (zero? fail) (zero? error))))

(defn test-cljs
  ([]
   (test-cljs nil))
  ([conf-or-id]
   (let [conf (prj.cljs/config (or conf-or-id :test))
         jsfile-path (get-in conf [:compiler :output-to])]
     (prj.cljs/build-cljs conf)
     (-> (prj.cljs/run-node jsfile-path)
         :wait-for
         deref
         zero?))))
