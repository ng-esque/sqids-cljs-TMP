(ns sqids-cljs.minlength-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [sqids-cljs.core :refer [Sqids]]
   [clojure.core.async :as casync]))

(deftest minlength-simple-test
  (testing "minLength : Simple test."
    (let [min-length (count (:alphabet (.defaultOptions (Sqids. {}))))
          id "86Rf07xd4zBmiJXQG6otHEbew02c3PWsUOLZxADhCpKj7aVFv9I8RquYrNlSTM"
          numbers [1 2 3]]
      (is (= id (.encode (Sqids. {:minLength min-length}) numbers)))
      (is (= numbers (.decode (Sqids. {:min-length min-length}) id))))))

(deftest minlength-incremental-test
  (testing "minLenght : Incremental test."
    (let [alphabet (:alphabet (.defaultOptions (Sqids. {})))
          numbers [1 2 3]
          ids  [[6 "86Rf07"]
                [7 "86Rf07x"]
                [8 "86Rf07xd"]
                [9 "86Rf07xd4"]
                [10 "86Rf07xd4z"]
                [11 "86Rf07xd4zB"]
                [12 "86Rf07xd4zBm"]
                [13 "86Rf07xd4zBmi"]
                [(+ 0 (count alphabet)) "86Rf07xd4zBmiJXQG6otHEbew02c3PWsUOLZxADhCpKj7aVFv9I8RquYrNlSTM"]
                [(+ 1 (count alphabet)) "86Rf07xd4zBmiJXQG6otHEbew02c3PWsUOLZxADhCpKj7aVFv9I8RquYrNlSTMy"]
                [(+ 2 (count alphabet)) "86Rf07xd4zBmiJXQG6otHEbew02c3PWsUOLZxADhCpKj7aVFv9I8RquYrNlSTMyf"]
                [(+ 3 (count alphabet)) "86Rf07xd4zBmiJXQG6otHEbew02c3PWsUOLZxADhCpKj7aVFv9I8RquYrNlSTMyf1"]]]
      (loop [i 0]
        (when (< i (count ids))
          (let [[min-length id] (nth ids i)]
            (is (= id (.encode (Sqids. {:minLength min-length}) numbers)))
            (is (= min-length (count (.encode (Sqids. {:minLength min-length}) numbers))))
            (is (= numbers (.decode (Sqids. {:minLength min-length}) id)))
            (recur (inc i))))))))

(deftest minlength-incremental-numbers-test
  (testing "minLenght : Incremental numbers test."
    (let [min-length (count (:alphabet (.defaultOptions (Sqids. {}))))
          ids  [["SvIzsqYMyQwI3GWgJAe17URxX8V924Co0DaTZLtFjHriEn5bPhcSkfmvOslpBu" [0 0]]
                ["n3qafPOLKdfHpuNw3M61r95svbeJGk7aAEgYn4WlSjXURmF8IDqZBy0CT2VxQc" [0 1]]
                ["tryFJbWcFMiYPg8sASm51uIV93GXTnvRzyfLleh06CpodJD42B7OraKtkQNxUZ" [0 2]]
                ["eg6ql0A3XmvPoCzMlB6DraNGcWSIy5VR8iYup2Qk4tjZFKe1hbwfgHdUTsnLqE" [0 3]]
                ["rSCFlp0rB2inEljaRdxKt7FkIbODSf8wYgTsZM1HL9JzN35cyoqueUvVWCm4hX" [0 4]]
                ["sR8xjC8WQkOwo74PnglH1YFdTI0eaf56RGVSitzbjuZ3shNUXBrqLxEJyAmKv2" [0 5]]
                ["uY2MYFqCLpgx5XQcjdtZK286AwWV7IBGEfuS9yTmbJvkzoUPeYRHr4iDs3naN0" [0 6]]
                ["74dID7X28VLQhBlnGmjZrec5wTA1fqpWtK4YkaoEIM9SRNiC3gUJH0OFvsPDdy" [0 7]]
                ["30WXpesPhgKiEI5RHTY7xbB1GnytJvXOl2p0AcUjdF6waZDo9Qk8VLzMuWrqCS" [0 8]]
                ["moxr3HqLAK0GsTND6jowfZz3SUx7cQ8aC54Pl1RbIvFXmEJuBMYVeW9yrdOtin" [0 9]]]]
      (loop [i 0]
        (when (< i (count ids))
          (let [[id numbers] (nth ids i)]
            (is (= id (.encode (Sqids. {:minLength min-length}) numbers)))
            (is (= numbers (.decode (Sqids. {:minLength min-length}) id)))
            (recur (inc i))))))))

(deftest minlength-min-lengths-test
  (testing "minLenght : min-lenghts test."
    (let [full-length (count (:alphabet (.defaultOptions (Sqids. {}))))
          min-lengths [0 1 5 10 full-length]
          all-numbers [[0]
                       [0 0 0 0 0]
                       [1 2 3 4 5 6 7 8 9 10]
                       [100 200 300]
                       [1000 2000 3000]
                       [1000000]
                       [(.maxValue (Sqids. {}))]]]
      (loop [i 0]
        (if (< i (count min-lengths))
          (let [min-length (nth min-lengths i)]
            (loop [j 0]
              (when (< j (count all-numbers))
                (let [numbers (nth all-numbers j)
                      id (.encode (Sqids. {:minLength min-length}) numbers)]
                  (is (= id (.encode (Sqids. {:minLength min-length}) numbers)))
                  (is (<= min-length (count (.encode (Sqids. {:minLength min-length}) numbers))))
                  (is (= numbers (.decode (Sqids. {:minLength min-length}) id)))
                  (recur (inc j))))))
          (recur (inc i)))))))

(defn >!! [ch value]
  (casync/go
    (casync/put! ch value)))

(deftest minlength-out-of-range-invalid-min-length-test
  (testing "minLenght : out of range invalid minLenght"
    (let [minlength-promise (casync/promise-chan)
          sqids (new Sqids {})
          max-value (.maxValue sqids)]
      (is (thrown? js/Error. (>!! minlength-promise (.encode (Sqids. {}) [-1]))))
      (is (thrown? js/Error. (>!! minlength-promise (.encode (Sqids. {}) [(inc max-value)])))))))
