(ns prj.pom
  (:refer-clojure :exclude [update read write])
  (:require [clojure.java.io :as io]
            [clojure.data.xml :as xml]
            [clojure.zip :as zip]))

(xml/alias-uri 'pom "http://maven.apache.org/POM/4.0.0")

(def group-id ::pom/groupId)
(def artifact-id ::pom/artifactId)
(def version ::pom/version)

(defn- make-xml-element
  [{:keys [tag attrs] :as node} content]
  (with-meta
    (apply xml/element tag attrs content)
    (meta node)))

(defn- zipper
  [root]
  (zip/zipper xml/element? :content make-xml-element root))

(defn- xml-update
  [root path replace-node]
  (let [z (zipper root)]
    (zip/root
     (loop [[tag & more-tags :as tags] path, parent z, child (zip/down z)]
       (if child
         (if (= tag (:tag (zip/node child)))
           (if (seq more-tags)
             (recur more-tags child (zip/down child))
             (zip/edit child (constantly replace-node)))
           (recur tags parent (zip/right child)))
         (zip/append-child parent replace-node))))))

(defn- xml-select
  [root path]
  (let [z (zipper root)]
    (loop [[tag & more-tags :as tags] path, parent z, child (zip/down z)]
      (when child
        (let [node (zip/node child)]
          (if (= tag (:tag node))
            (if (seq more-tags)
              (recur more-tags child (zip/down child))
              node)
            (recur tags parent (zip/right child))))))))

(defn update
  [pom-file orders]
  (let [pom (with-open [r (io/reader pom-file)]
              (reduce (fn [p [path value]]
                        (xml-update p path (xml/sexp-as-element [(last path) value])))
                      (xml/parse r :include-node? #{:element :characters :comment})
                      orders))]
    (spit pom-file (xml/indent-str pom))))

(defn select
  [pom-file orders]
  (with-open [r (io/reader pom-file)]
    (let [p (xml/parse r :include-node? #{:element :characters :comment})]
          (reduce (fn [ctx [key path]]
                    (->>
                     {key (-> (xml-select p path)
                              :content
                              first)}
                     (merge ctx)))
                  {}
                  orders))))

