(ns prj.task.package
  (:require [prj :refer [deftask]]
            [prj.package]))

(deftask update-pom
  [args]
  (prj.package/update-pom args)
  )

(deftask deploy
  [args]
  (prj.package/deploy args))


