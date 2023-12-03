;; This test runner is intended to be run from the command line
(ns sqids-cljs.test-runner
  (:require
   [sqids-cljs.alphabet-test]
   [sqids-cljs.blocklist-test]
   [sqids-cljs.encoding-test]
   [sqids-cljs.minlength-test]
   [figwheel.main.testing :refer [run-tests]]))

(defn -main []
  (run-tests
   'sqids-cljs.alphabet-test
   'sqids-cljs.encoding-test
   'sqids-cljs.blocklist-test
   'sqids-cljs.minlength-test))
