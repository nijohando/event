(ns prj.package
  (:require [clojure.java.io :as io]
            [cemerick.pomegranate.aether :as aether]
            [prj]
            [prj.pom :as pom]))

(defn- snapshot?
  [version]
  (.endsWith version "-SNAPSHOT"))

(defn update-pom
  [[group-id artifact-id version]]
  (pom/update (io/file "pom.xml")
              {[pom/group-id] group-id
               [pom/artifact-id] artifact-id
               [pom/version] version}))

(defn deploy
  [[pom-file jar-file deploy-repo-url]]
  (java.lang.System/setProperty "aether.checksums.forSignature" "true")
  (aether/register-wagon-factory!
   "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))
  (let [{:keys [group-id artifact-id version]} (pom/select (io/file pom-file)
                                                           {:group-id [pom/group-id]
                                                            :artifact-id [pom/artifact-id]
                                                            :version [pom/version]})
        coordinates [(symbol group-id artifact-id) version]
        artifact-map (merge {}
                            (when-not (snapshot? version)
                              {[:extension "pom.asc"] (io/file (str pom-file ".asc"))
                               [:extension "jar.asc"] (io/file (str jar-file ".asc"))}))
        repository {:default {:url deploy-repo-url
                              :username (prj/env :deploy-repo-user)
                              :password (prj/env :deploy-repo-pass)}}]
    (aether/deploy :coordinates coordinates
                   :artifact-map artifact-map
                   :jar-file jar-file
                   :pom-file pom-file
                   :repository repository)))
