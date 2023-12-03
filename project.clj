(defproject sqids-cljs "1.0.0"
  :description "A small library that lets you generate unique IDs from numbers."
  :url "https://github.com/sqids/sqids-cljs"
  :license {:name "MIT"
            :url "https://github.com/sqids/sqids-clojure/blob/main/LICENSE"}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.11.4"]
                 [org.clojure/core.async "1.6.681"]
                 ]
  :source-paths ["src" "test"]

  :aliases {"fig:build" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
            "fig:min"   ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "dev"]
            "fig:test"  ["run" "-m" "figwheel.main" "-co" "test.cljs.edn" "-m" "sqids-cljs.test-runner"]}

  :plugins [[lein-cljfmt "0.8.0"]
            [com.github.clj-kondo/lein-clj-kondo "0.2.5"]]
  :profiles {:dev {:dependencies [[com.bhauman/figwheel-main "0.2.17"]
                                  [org.slf4j/slf4j-nop "1.7.30"]
                                  [com.bhauman/rebel-readline-cljs "0.1.4"]]
                   
                   :resource-paths ["target"]
                   :clean-targets ^{:protect false} ["target"]}})

