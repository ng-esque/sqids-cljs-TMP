# [Sqids ClojureScript](https://sqids.org/sqids-cljs)

[Sqids](https://sqids.org/clojurescript) (*pronounced "squids"*) is a small library that lets you **generate unique IDs from numbers**. It's good for link shortening, fast & URL-safe ID generation and decoding back into numbers for quicker database lookups.

Features:

- **Encode multiple numbers** - generate short IDs from one or several non-negative numbers
- **Quick decoding** - easily decode IDs back into numbers
- **Unique IDs** - generate unique IDs by shuffling the alphabet once
- **ID padding** - provide minimum length to make IDs more uniform
- **URL safe** - auto-generated IDs do not contain common profanity
- **Randomized output** - Sequential input provides nonconsecutive IDs
- **Many implementations** - Support for [40+ programming languages](https://sqids.org/)

## 🧰 Use-cases

Good for:

- Generating IDs for public URLs (eg: link shortening)
- Generating IDs for internal systems (eg: event tracking)
- Decoding for quicker database lookups (eg: by primary keys)

Not good for:

- Sensitive data (this is not an encryption library)
- User IDs (can be decoded revealing user count)

## 🚀 Getting started

Install Sqids via [Clojars](https://clojars.org/)

In your project.clj ***:dependencies [ ]****:

```
  [sqids-cljs "1.0.0"]
```

Clone this repo:

```bash
git clone https://github.com/sqids/sqids-cljs
cd sqids-cljs
lein fig:test
```
The main Sqids library is in src/sqids_cljs/core.cljs; unit tests are in test/sqids_cljs/.

Use the following to format & check changes:
```bash
lein cljfmt
lein clj-kondo 
```

## 👩‍💻 Examples

Simple encode & decode:

```cljourecript
(let [sqids (new Sqids {})
      id (.encode sqids [1 2 3]) ;; => "86Rf07
      numbers (.decode sqids id)]) ;; => [1 2 3]
```

Due to multi-arity being unsupported by ***(deftype )*** an ***options*** argument must be passed at initialisation.  Empty uses defaults.
```clojurescript
(let [sqids (Sqids. {})]) ;; use default-options
```

> **Note**
> 🚧 Because of the algorithm's design, **multiple IDs can decode back into the same sequence of numbers**. If it's important to your design that IDs are canonical, you have to manually re-encode decoded numbers and check that the generated ID matches.

Enforce a *minimum* length for IDs:

```clojurecript
(let [sqids (new Sqids {:minLength 10})
      id (.encode sqids [1 2 3]) ;; => "86Rf07xd4z"
      numbers (.decode sqids id)]) ;; => [1 2 3]
```

Randomize IDs by providing a custom alphabet:

```clojurecript
(let [sqids (new Sqids {:alphabet: "FxnXM1kBN6cuhsAvjW3Co7l2RePyY8DwaU04Tzt9fHQrqSVKdpimLGIJOgb5ZE"})
      id (.encode sqids [1 2 3]) ;; => "B4aajs"
      numbers (.decode sqids id)]) ;; => [1 2 3]
```

Prevent specific words from appearing anywhere in the auto-generated IDs:

```clojurescript
(let [sqids (new Sqids {:blocklist (set ["86Rf07"]){)
      id (.encode sqids [1 2 3]) ;; => "se8ojk"
      numbers (.decode sqids id)]) ;; => [1 2 3]
```

## 📝 License

[MIT](LICENSE)
