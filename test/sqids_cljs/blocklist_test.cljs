(ns sqids-cljs.blocklist-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [sqids-cljs.core :refer [Sqids]]
   [clojure.core.async :as casync]))

(deftest blocklist-no-custom-blocklist-test
  (testing "Blocklist : No custom blocklist param, use the default blocklist"
    (is (= [4572721] (.decode (Sqids. {}) "aho1e")))
    (is (= "JExTR" (.encode (Sqids. {}) [4572721])))))

(deftest blocklist-if-an-empty-blocklist-param-passed-test
  (testing "Blocklist : if an empty blocklist param passed, don't use any blocklist"
    (is (= [4572721] (.decode (Sqids. {:blocklist (set [])}) "aho1e")))
    (is (= "aho1e" (.encode (Sqids. {:blocklist #{}}) [4572721])))))

(deftest blocklist-a-non-empty-blocklist-param-passed-test
  (testing "Blocklist : if a non-empty blocklist param passed, use only any that"
    ;; make sure we don't use the default blocklist
    (is (= [4572721] (.decode (Sqids. {:blocklist (set ["ArUO"])}) "aho1e")))
    (is (= "aho1e" (.encode (Sqids. {:blocklist #{"ArUO"}}) [4572721])))
    ;; make sure we are using the passed blocklist
    (is (= [100000] (.decode (Sqids. {:blocklist (set ["ArUO"])}) "ArUO")))
    (is (= "QyG4" (.encode (Sqids. {:blocklist (set ["ArUO"])}) [100000])))
    (is (= [100000] (.decode (Sqids. {:blocklist (set ["ArUO"])}) "QyG4")))))

(deftest blocklist-blocklist-test
  (testing "Blocklist : Blocklist test."
    (let [blocklist #{"JSwXFaosAN" ;; normal result of 1st encoding, let's block that word on purpose
                      "OCjV9JK64o" ;; result of 2nd encoding
                      "rBHf"       ;; result of 3rd encoding is `4rBHfOiqd3`, let's block a substring
                      "79SM"       ;; result of 4th encoding is `dyhgw479SM`, let's block the postfix
                      "7tE6"}]     ;; result of 4th encoding is `7tE6jdAHLe`, let's block the prefix
      (is (= [1000000 2000000] (.decode (Sqids. {:blocklist blocklist}) "1aYeB7bRUt")))
      (is (= "1aYeB7bRUt" (.encode (Sqids. {:blocklist blocklist}) [1000000 2000000]))))))

(deftest blocklist-decoding-blocklist-words-test
  (testing "Blocklist : decoding blocklist words should still work."
    (let [blocklist #{"86Rf07" "se8ojk" "ARsz1p" "Q8AI49" "5sQRZO"}]
      (is (= [1 2 3] (.decode (Sqids. {:blocklist blocklist}) "86Rf07")))
      (is (= [1 2 3] (.decode (Sqids. {:blocklist blocklist}) "se8ojk")))
      (is (= [1 2 3] (.decode (Sqids. {:blocklist blocklist}) "ARsz1p")))
      (is (= [1 2 3] (.decode (Sqids. {:blocklist blocklist}) "Q8AI49")))
      (is (= [1 2 3] (.decode (Sqids. {:blocklist blocklist}) "5sQRZO"))))))

(deftest blocklist-match-against-a-short-blocklist-word-test
  (testing "Blocklist : Match against a short blocklist word."
    (is (= [1000] (.decode (Sqids. {:blocklist #{"pnd"}}) (.encode (Sqids. {:blocklist #{"pnd"}}) [1000]))))))

(deftest blocklist-filtering-in-constructor-test
  (testing "Blocklist : filtering in constructor."
    (let [id (.encode
              (Sqids. {:alphabet "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                       :blocklist #{"sxnzkl"}}) ;; lowercase blocklist in only-uppercase alphabet
              [1 2 3])
          numbers (.decode
                   (Sqids. {:alphabet "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                            :blocklist #{"sxnzkl"}}) ;; lowercase blocklist in only-uppercase alphabet
                   id)]
      (is (= "IBSHOZ" id)) ;; without blocklist, would've been "SXNZKL"
      (is (= [1 2 3] numbers)))))

(defn >!! [ch value]
  (casync/go
    (casync/put! ch value)))

(deftest blocklist-max-encoding-attempts-test
  (testing "Blocklist : max encoding attempts test"
    (let [blocklist-promise (casync/promise-chan)
          alphabet "abc"
          min-length 3
          blocklist #{"cab" "abc" "bca"}
          sqids (new Sqids {:alphabet alphabet :minLength min-length :blocklist blocklist})
          max-value (.maxValue sqids)]
      (is (thrown? js/Error. (>!! blocklist-promise (.encode sqids [(inc max-value)])))))))
