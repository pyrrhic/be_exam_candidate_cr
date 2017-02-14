(defproject scoir "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clojure-csv/clojure-csv "2.0.1"]
                 [org.clojure/data.json "0.2.6"]
                 [hawk "0.2.11"]
                 [commons-io/commons-io "2.5"]]
  :profiles {:uberjar {:aot :all}}
  :main be-exam-candidate-cr.core
  :java-source-paths ["test/mock"]
  )
