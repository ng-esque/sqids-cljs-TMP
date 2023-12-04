(ns ^:figwheel-hooks sqids-cljs.core
  (:require
   [sqids-cljs.blocklist :as bl]
   [clojure.string :as cstr]))

	                        ;; url-safe characters
(def default-options {:alphabet "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
	                        ;; `minLength` is the minimum length IDs should be (`u8` type)
                      :minLength 0
	                        ;; a list of words that should not appear anywhere in the IDs
                      :blocklist bl/default-blocklist})

(defn contains-multibyte? [^String s] (some #(> (.charCodeAt %) 127) s))

(defn alphabet-shuffle [^String alphabet]
  ;; consistent shuffle (always produces the same result given the input)
  (let [achars (vec (seq alphabet))]
    (loop [i 0
           j (dec (count achars))
           loop-achars achars]
      (if (< j 1)
        (apply str loop-achars)
        (let [r (mod (+ (* i j) (.charCodeAt (first (str (nth loop-achars i)))) (.charCodeAt (first (str (nth loop-achars j))))) (count loop-achars))]
          (recur (inc i) (dec j) (assoc loop-achars i (nth loop-achars r) r (nth loop-achars i))))))))

(defn to-id [^Integer numb ^String alphabet]
  (let [achars (vec (seq alphabet))
        id (loop [result numb
                  id (if (= 0 numb)
                       (cons (nth achars (mod numb (count achars))) [])
                       [])]
             (if (> result 0)
               (recur
                (int (/ result (count achars)))
                (cons (nth achars (mod result (count achars))) id))
               id))]
    (apply str id)))

(defn to-number
  [^String id ^String alphabet]
  (loop [i 0 a 0]
    (if (< i (count (seq id)))
      (let [v (nth (seq id) i)
            nu-a (+ (* a (count alphabet)) (cstr/index-of alphabet v))]
        (recur (inc i) nu-a)) a)))

(defn init
  "Psuedo -init Object function. Due to (deftype obj Object) lacking an -init function, and to avoid using a factory function.
  Rather than being called when the Object is initialised: (Sqids. {});
  this function is called before any methods that use the Object's argument 'options'."
  [options]
  (let [alphabet (or (:alphabet options) (:alphabet default-options))
        min-length (or (:minLength options) (:minLength default-options))
        blocklist (or (:blocklist options) (:blocklist default-options))
          ;; clean up blocklist:
          ;; 1. all blocklist words should be lowercase
          ;; 2. no words less than 3 chars
          ;; 3. if some words contain chars that are not in the alphabet, remove those
        filtered-blocklist (loop [loop-blocklist blocklist
                                  loop-filtered #{}]
                             (if (seq loop-blocklist)
                               (let [word (first loop-blocklist)]
                                 (if (<= 3 (count word))
                                   (let [word-chars (seq (cstr/lower-case word))
                                         intersection (filter #(contains? (set (seq (cstr/lower-case alphabet))) %) word-chars)]
                                     (if (= (count intersection) (count word-chars))
                                       (recur (rest loop-blocklist)
                                              (conj loop-filtered (cstr/lower-case word)))
                                       (recur (rest loop-blocklist)
                                              loop-filtered)))
                                   (recur (rest loop-blocklist)
                                          loop-filtered)))
                               loop-filtered))
        sqids-options {:alphabet (alphabet-shuffle alphabet) :minLength min-length :blocklist filtered-blocklist}]
    (when (contains-multibyte? alphabet)
      (throw (js/Error. "Alphabet cannot contain multibyte characters")))
    (when (< (count alphabet) 3)
      (throw (js/Error. "Alphabet length must be at least 3")))
    (when (not= (count alphabet) (count (set alphabet)))
      (throw (js/Error. "Alphabet must contain unique characters")))
    (when (or (not (number? min-length)) (< min-length 0) (> min-length 255))
      (throw (js/Error. (str "Minimum length has to be between 0 and 255"))))
    sqids-options))

;;  This function is for the (cstr/escape ) used in (decode ) on the character separator before being passed to (re-pattern ).
;;    SEE TEST : alphabet-long-test
;;      ex. ERR without the escape character map : [Figwheel] SyntaxError: unmatched ')' in regular expression <empty string>
;;         When you know ahead of a time a delimiter you want to use to split a string you use the # special character to identify the string as a regex, 
;;            ex. (cstr/split 'haz_zah' #'_')
;;         Due to sqids-spec using a variable delimiter utilizing the first character from the current alphabet you can call (re-pattern STR) to generate the #'STR' regex.
;;            ex. (let [spl-str '_'] (cstr/split 'haz_zah' (re-pattern spl-str))) => ['haz' 'zah']
;;         Clojure's strings and regex's have special characters that require additional escaping when used with (re-pattern ).
;;            ex. (cstr/split 'haz_zah' (re-pattern '_')) => ['haz' 'zah']
;;                (cstr/split 'haz$zah' (re-pattern '$')) => ['haz$zah']
;;                (cstr/split 'haz$zah' (re-pattern '\\$')) => ['haz' 'zah']
;;         Below is a character map of each of characters from the alphabet-long-test string that require additional escaping when used with (re-pattern ).
;;  https://clojuredocs.org/clojure.string/escape
;;    (escape s cmap)   Return a new string, using cmap to escape each character ch from s as follows:
;;                        If (cmap ch) is nil, append ch to the new string.
;;                        If (cmap ch) is non-nil, append (str (cmap ch)) instead.")
(defn separator-escape
  [^String s]
  (let [cmap {\$ "\\$"    \^ "\\^"    \* "\\*"    \( "\\("
              \) "\\)"    \+ "\\+"    \| "\\|"    \{ "\\{"
              \[ "\\["    \? "\\?"    \. "\\."    \\ "\\\\"}]
    (cstr/escape s cmap)))

;; (deftypes ) are unable to utilise multi-arity, so every (new ) call must accompany a map, an empty map loads defaults: (Sqids. {})=>[Object object] ; (Sqids.)=>ArityError
(deftype Sqids [options]
  Object

;Public method to get default options. This method is used in the tests, ex. test/sqids_cljs/minlength_test.cljs:minlength-simple-test
  (defaultOptions [_] default-options)

;;  Returns bool. (some ) returns true|nil, use (not= ) to make true|false
;;     true ; (not= nil true)=>true
;;    false : (not= nil  nil)=>false
;;  https://clojuredocs.org/clojure.core/some
;;  (some pred coll) Returns the first logical true value of (pred x) for any x in coll, else nil."
  (isBlockedId
    [_ ^String id]
    (let [id (cstr/lower-case id)
          blocklist (:blocklist (init options))];(:options @(.state this)))]
      (not= nil (some #(let [word %]
                       ;; no point in checking words that are longer than the ID
                         (when (<= (count word) (count id))
                           (cond
                           ;; short words have to match completely; otherwise, too many matches
                             (or (<= (count id) 3) (<= (count word) 3))
                             (= id word)
                           ;; words with leet speak replacements are visible mostly on the ends of the ID
                             (re-matches #"\d" word)
                             (or (cstr/starts-with? id word)
                                 (cstr/ends-with? id word))
                             :else ;; otherwise, check for blocked word anywhere in the string
                             (cstr/includes? id word))))
                      blocklist))))

;;  Internal function that encodes an array of unsigned integers into an ID
;;    @param {array.<number>} numbers Non-negative integers to encode into an ID
;;    @param {number} increment An internal number used to modify the `offset` variable in order to re-generate the ID
;;    @returns {string} Generated ID"
  (encodeNumbers
    [this ^clojure.lang.PersistentVector numbers ^js/Number increment]
    (let [initialised-options (init options)
          alphabet (:alphabet initialised-options)
          min-length (:minLength initialised-options)]
      (when (> increment (count alphabet))
        (throw (js/Error. "Reached max attempts to re-generate the ID")))
           ;; get a semi-random offset from input numbers
      (let [offset (mod (+ (loop [loop-a (count numbers)
                                  i 0]
                             (if (< i (count numbers))
                               (recur (+ loop-a i (.charCodeAt (nth alphabet (mod (nth numbers i) (count alphabet)))))
                                      (inc i))
                               (mod loop-a (count alphabet))))
                           increment) (count alphabet))
           ;; re-arrange alphabet so that second-half goes in front of the first-half
            enc-alphabet (str (subs alphabet offset) (subs alphabet 0 offset))
           ;; `prefix` is the first character in the generated ID, used for randomization
            prefix (first enc-alphabet)
           ;; reverse alphabet (otherwise for [0, x] `offset` and `separator` will be the same char)
            rev-alphabet (apply str (reverse (seq enc-alphabet)))
           ;; encode input array
            [encode-id encode-alphabet] (loop [i 0
                                              ;; final ID will always have the `prefix` character at the beginning
                                               loop-ret [prefix]
                                               loop-alphabet rev-alphabet]
                                         ;; encode input array
                                          (if (< i (count numbers))
                                            (let [numb (int (nth numbers i))
                                                 ;; the first character of the alphabet is going to be reserved for the `separator`
                                                  alphabet-without-separator (subs loop-alphabet 1)
                                                  numb-id (to-id numb alphabet-without-separator)
                                                 ;; if not the last number
                                                  nu-ret (if (< i (- (count numbers) 1))
                                                          ;; `separator` character is used to isolate numbers within the ID
                                                           (conj (conj loop-ret numb-id) (subs loop-alphabet 0 1))
                                                           (conj loop-ret numb-id))
                                                 ;; shuffle on every iteration
                                                  nu-alphabet (if (< i (- (count numbers) 1))
                                                                (alphabet-shuffle loop-alphabet)
                                                                loop-alphabet)]
                                              (recur (inc i)
                                                     nu-ret
                                                     nu-alphabet))
                                           ;; join all the parts to form an ID
                                            [(cstr/join loop-ret) loop-alphabet]))
           ;; handle `minLength` requirement, if the ID is too short
            id-min-length (if (> min-length (count encode-id))
                           ;; keep appending `separator` + however much alphabet is needed
                           ;; for decoding: two separators next to each other is what tells us the rest are junk characters
                            (loop [loop-id (str encode-id (first encode-alphabet)) ;; append a separator
                                   loop-alphabet encode-alphabet]
                              (if (<= min-length (count loop-id))
                                loop-id
                                (let [shuffled-alphabet (alphabet-shuffle loop-alphabet)
                                      slice-length (min (- min-length (count loop-id)) (count shuffled-alphabet))]
                                  (recur (str loop-id (subs shuffled-alphabet 0 slice-length))
                                         shuffled-alphabet))))
                            encode-id)
           ;; if ID has a blocked word anywhere, restart with a +1 increment
            id (if (.isBlockedId this id-min-length)
                 (.encodeNumbers this numbers (inc (float increment)))
                 id-min-length)]
        id)))

;;  Encodes an array of unsigned integers into an ID
;;    These are the cases where encoding might fail:
;;    - One of the numbers passed is smaller than 0 or greater than `maxValue()`
;;    - An n-number of attempts has been made to re-generated the ID, where n is alphabet length + 1
;;    @param {array.<number>} numbers Non-negative integers to encode into an ID
;;    @returns {string} Generated ID
  (encode [this ^clojure.lang.PersistentVector numbers]
    (if (empty? numbers) "" ;; if no numbers passed, return an empty string
        (let [in-range-numbers (filter #(and (>= % 0) (<= % (.maxValue this))) numbers)]
          (when (not= (count numbers) (count in-range-numbers))
            (throw (js/Error. (str "Encoding supports numbers between 0 and " (.maxValue this)))))
          (.encodeNumbers this numbers 0))))

;; Decodes an ID back into an array of unsigned integers
;;  These are the cases where the return value might be an empty array:
;;  - Empty ID / empty string
;;  - Non-alphabet character is found within ID
;;  @param {string} id Encoded ID
;;  @returns {array.<number>} Array of unsigned integers 
  (decode
    [_ ^String id]
    (if (= id "") [] ;; if an empty string, return an empty array
        (let [alphabet (:alphabet (init options))]
          ;; (not (every? #(contains? (set (seq "abc")) %) (seq "bca"))) => false
          ;; (not (every? #(contains? (set (seq "abc")) %) (seq "bcaF"))) => true
          (if (not (every? #(contains? (set (seq alphabet)) %) (seq id)))
            [] ;; if a character is not in the alphabet, return an empty array
            (let [prefix (.charAt id 0) ;; first character is always the `prefix`
                 ;; `offset` is the semi-random position that was generated during encoding
                  offset (cstr/index-of alphabet prefix)]
             ;; decode
              (loop [loop-id (subs id 1) ;; now it's safe to remove the prefix character from ID, it's not needed anymore
                    ;; re-arrange alphabet back into it's original form and reverse alphabet
                     loop-alphabet (cstr/join (reverse (str (subs alphabet offset) (subs alphabet 0 offset))))
                     loop-ret []]
                (if (not= loop-id "")
                  (let [separator (.charAt loop-alphabet 0)
                       ;; we need the first part to the left of the separator to decode the number
                        chunks (cstr/split loop-id (re-pattern (separator-escape (str separator))))
                        alphabet-without-separator (subs loop-alphabet 1)]
                    (if (and
                         (< 0 (count chunks))
                         (not= (first chunks) ""))
                           ;; decode the number without using the `separator` character
                      (let [nu-ret  (conj loop-ret (to-number (first chunks) alphabet-without-separator))
                           ;; if this ID has multiple numbers, shuffle the alphabet because that's what encoding function did
                            nu-alphabet (if (> (count chunks) 1)
                                          (alphabet-shuffle loop-alphabet)
                                          loop-alphabet)
                           ;; `id` is now going to be everything to the right of the `separator
                            nu-id (cstr/join separator (rest chunks))]
                        (recur nu-id nu-alphabet nu-ret))
                      loop-ret))
                  loop-ret)))))))

;; this should be the biggest unsigned integer that the language can safely/mathematically support
;; the spec does not specify the upper integer limit - so it's up to the individual programming languages
;; examples as of 2023-09-24:
;; golang: uint64
;; rust: u128
;; php: PHP_INT_MAX
  (maxValue [_] (- (Math/pow 2 31) 1)))  ;; >(2**31)-1=>ERR : Number.MAX_SAFE_INTEGER was causing errors, it seems clojujre's 2**31-1 max applies here too
