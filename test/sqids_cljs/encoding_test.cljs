(ns sqids-cljs.encoding-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [sqids-cljs.core :refer [Sqids]]
   [clojure.core.async :as casync]))

(deftest encoding-simple-test
  (testing "Encoding : Simple test."
    (let [id "86Rf07"
          numbers [1 2 3]]
      (is (= id (.encode (Sqids. {}) numbers)))
      (is (= numbers (.decode (Sqids. {}) id))))))

(deftest encoding-different-inputs-test
  (testing "Encoding : Different inputs test."
    (let [numbers [0 0 0 1 2 3 100 1000 100000 1000000 (.maxValue (Sqids. {}))]]
      (is (= numbers (.decode (Sqids. {}) (.encode (Sqids. {}) numbers)))))))

(deftest encoding-incremental-numbers-test
  (testing "Encoding : Incremental numbers test."
    (let [ids  [["bM" [0]]
                ["Uk" [1]]
                ["gb" [2]]
                ["Ef" [3]]
                ["Vq" [4]]
                ["uw" [5]]
                ["OI" [6]]
                ["AX" [7]]
                ["p6" [8]]
                ["nJ" [9]]]]
      (loop [i 0]
        (when (< i (count ids))
          (let [[id numbers] (nth ids i)]
            (is (= id (.encode (Sqids. {}) numbers)))
            (is (= numbers (.decode (Sqids. {}) id)))
            (recur (inc i))))))))

(deftest encoding-incremental-numbers-same-index-0-test
  (testing "Encoding : Incremental-numbers, same index 0 test."
    (let [ids  [["SvIz" [0 0]]
                ["n3qa" [0 1]]
                ["tryF" [0 2]]
                ["eg6q" [0 3]]
                ["rSCF" [0 4]]
                ["sR8x" [0 5]]
                ["uY2M" [0 6]]
                ["74dI" [0 7]]
                ["30WX" [0 8]]
                ["moxr" [0 9]]]]
      (loop [i 0]
        (when (< i (count ids))
          (let [[id numbers] (nth ids i)]
            (is (= id (.encode (Sqids. {}) numbers)))
            (is (= numbers (.decode (Sqids. {}) id)))
            (recur (inc i))))))))

(deftest encoding-incremental-numbers-same-index-1-test
  (testing "Encoding : Incremental-numbers, same index 1 test."
    (let [ids  [["SvIz" [0 0]]
                ["nWqP" [1 0]]
                ["tSyw" [2 0]]
                ["eX68" [3 0]]
                ["rxCY" [4 0]]
                ["sV8a" [5 0]]
                ["uf2K" [6 0]]
                ["7Cdk" [7 0]]
                ["3aWP" [8 0]]
                ["m2xn" [9 0]]]]
      (loop [i 0]
        (when (< i (count ids))
          (let [[id numbers] (nth ids i)]
            (is (= id (.encode (Sqids. {}) numbers)))
            (is (= numbers (.decode (Sqids. {}) id)))
            (recur (inc i))))))))

(deftest encoding-multi-input-test
  (testing "Encoding : Multi input test."
    (let [numbers [0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25
                   26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49
                   50 51 52 53 54 55 56 57 58 59 60 61 62 63 64 65 66 67 68 69 70 71 72 73
                   74 75 76 77 78 79 80 81 82 83 84 85 86 87 88 89 90 91 92 93 94 95 96 97
                   98 99]]
      (is (= (.decode (Sqids. {}) (.encode (Sqids. {}) numbers)) numbers)))))

(deftest encoding-no-numbers-test
  (testing "Encoding : No numbers test."
    (let [numbers []]
      (is (= "" (.encode (Sqids. {}) numbers))))))

(deftest encoding-decoding-empty-string-test
  (testing "Encoding : Decoding empty string test."
    (let [numbers []]
      (is (= numbers (.decode (Sqids. {}) ""))))))

(deftest encoding-decoding-with-an-id-with-an-invalid-character-test
  (testing "Encoding : Decoding an ID with an invalid character test."
    (let [numbers []]
      (is (= numbers (.decode (Sqids. {}) "*"))))))

(defn >!! [ch value]
  (casync/go
    (casync/put! ch value)))

(deftest encoding-out-of-range-numbers-test
  (testing "Encoding : out of range numbers"
    (let [encode-promise (casync/promise-chan)
          sqids (new Sqids {})
          max-value (.maxValue sqids)]
      (is (thrown? js/Error. (>!! encode-promise (.encode (Sqids. {}) [-1]))))
      (is (thrown? js/Error. (>!! encode-promise (.encode (Sqids. {}) [(inc max-value)])))))))
