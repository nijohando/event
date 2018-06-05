(ns prj.task.package
  (:require [jp.nijohando.prj.core :refer [env deftask]]
            [jp.nijohando.prj.package :as prj-package]))

(deftask update-pom
  [[pom-file group-id artifact-id version]]
  (prj-package/update-pom
    pom-file
    {:group-id group-id
     :artifact-id artifact-id
     :version version}))

(deftask deploy
  [[pom-file jar-file deploy-repo-url]]
  (prj-package/deploy
   {:pom-file pom-file
    :jar-file jar-file
    :repository {:url deploy-repo-url
                 :username (env :deploy-repo-user)
                 :password (env :deploy-repo-pass)}}))

(deftask install
  [[pom-file jar-file local-repo-path]]
  (prj-package/install
   {:pom-file pom-file
    :jar-file jar-file
    :local-repo-path local-repo-path}))


