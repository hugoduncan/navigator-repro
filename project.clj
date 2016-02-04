(defproject list-repro "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.omcljs/om "1.0.0-alpha30"]]
  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-6"]]
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]
                        :figwheel true
                        :compiler {:main "navigator-repro.core"
                                   :output-to "resources/public/js/compiled/app_web.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :optimizations :none
                                   :source-map-timestamp true}}]}
  :profiles
  {:dev
   {:dependencies [[figwheel-sidecar "0.5.0-6"]
                   [com.cemerick/piggieback "0.2.1"]
                   [weasel "0.7.0"]
                   [acyclic/squiggly-clojure "0.1.4"]]
    :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "target"]
  )
