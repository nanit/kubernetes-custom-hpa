(defproject custom-hpa "1.0.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "1.3.610"]
                 [clj-time "0.15.2"]
                 [io.kubernetes/client-java "8.0.2"]
                 [org.clojure/data.json "1.0.0"]
                 [http-kit "2.4.0"]
                 [clj-commons/iapetos "0.1.11"]
                 [compojure "1.6.2"]
                 [com.taoensso/timbre "4.10.0"]]

  :main ^:skip-aot custom-hpa.core

  :target-path "target/%s"

  :repl-options {:init-ns user
                 :timeout 120000}

  :profiles {:uberjar {:aot :all}
             :dev     {:source-paths ["dev"]
                       :dependencies [[org.clojure/tools.namespace "1.0.0"]
                                      [org.clojure/tools.nrepl "0.2.13"]]}
             :test    {:source-paths ["test"]
                       :dependencies [[org.clojure/tools.namespace "1.0.0"]
                                      [org.clojure/tools.nrepl "0.2.13"]]}})
