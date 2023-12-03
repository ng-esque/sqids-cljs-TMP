(ns sqids-cljs.alphabet-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [sqids-cljs.core :refer [Sqids]]))

(deftest alphabet-simple-test
  (testing "Alphabet : Simple test."
    (let [id "489158"
          numbers [1 2 3]]
      (is (= (.encode (Sqids. {:alphabet "0123456789abcdef"}) numbers) id))
      (is (= (.decode (Sqids. {:alphabet "0123456789abcdef"}) id) numbers)))))

(deftest alphabet-short-test
  (testing "Alphabet : Small alphabet test."
    (let [numbers [1 2 3]]
      (is (= (.decode (Sqids. {:alphabet "abc"}) (.encode (Sqids. {:alphabet "abc"})  numbers)) numbers)))))

(deftest alphabet-long-test
  (testing "Alphabet : Long alphabet test."
    (let [alphabet "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@;#$%^&*()-_+|{}[]:'\"/?.>,<`~"
          numbers [1 2 3]]
      (is (= (.decode (Sqids. {:alphabet alphabet}) (.encode (Sqids. {:alphabet alphabet})  numbers)) numbers)))))

(deftest alphabet-multibyte-characters-test
  (testing "Alphabet : Multibyte characters test."
    (let [alphabet "ë1092"]
      (is (thrown? js/Error. (.encode (Sqids. {:alphabet alphabet}) [1 2 3]))))))

(deftest alphabet-repeating-characters-test
  (testing "Alphabet : Repeating characters test."
    (let [alphabet "aabcdefg"]
      (is (thrown? js/Error. (.encode (Sqids. {:alphabet alphabet}) [1 2 3]))))))

(deftest alphabet-too-short-test
  (testing "Alphabet : Too short test."
    (let [alphabet "ab"]
      (is (thrown? js/Error. (.encode (Sqids. {:alphabet alphabet}) [1 2 3]))))))
