(ns mt-cljs.cas-atom
  (:refer-clojure :exclude [atom compare-and-set!]))

(def AtomicReference (.type js/Java "java.util.concurrent.atomic.AtomicReference"))

(defn- box [x]
  (.to js/Java #js [x] "java.lang.Object[]"))

(defn- unbox [x]
  (aget (.from js/Java x) 0))

(defprotocol ICompareAndSet
  (-compare-and-set! [a old-val new-val]))

(defn- validator-impl
  [validator new-val]
  (when (some? validator)
    (when-not (validator new-val)
      (throw (js/Error. "Validator rejected reference state")))))

(defn- watches-impl
  [o watches old-val new-val]
  (when (some? watches)
    (-notify-watches o old-val new-val))
  )
(defn- swap-impl
  [o atomic-reference validator watches f & args]
  (loop [old-boxed-val (.get atomic-reference)]
    (let [old-val (unbox old-boxed-val)
          new-val (apply f old-val args)]
      (validator-impl validator new-val)
      (let [set? (.compareAndSet atomic-reference old-boxed-val (box new-val))]
        (if set?
          (do
            (watches-impl o watches old-val new-val)
            new-val)
          (recur (.get atomic-reference)))))))

(deftype CasAtom [atomic-reference meta validator watches]
  Object
  (equiv [this other]
    (-equiv this other))

  IAtom

  ISwap
  (-swap! [o f] (swap-impl o atomic-reference validator watches f nil))
  (-swap! [o f a] (swap-impl o atomic-reference validator watches f a))
  (-swap! [o f a b] (swap-impl o atomic-reference validator watches f a b))
  (-swap! [o f a b xs] (apply swap-impl o atomic-reference validator watches f a b xs))

  IReset
  (-reset! [o new-val]
    (validator-impl validator new-val)
    (let [old-val (unbox (.get atomic-reference))]
      (.set atomic-reference (box new-val))
      (watches-impl o watches old-val new-val)
      new-val))

  ICompareAndSet
  (-compare-and-set! [o old-val new-val]
    (validator-impl validator new-val)
    (let [old-boxed-val (.get atomic-reference)
          ret           (if (= old-val (unbox old-boxed-val))
                          (.compareAndSet atomic-reference old-boxed-val (box new-val))
                          false)]
      (when ret
        (watches-impl o watches old-val new-val))
      ret))

  IEquiv
  (-equiv [o other] (identical? o other))

  IDeref
  (-deref [_] (unbox (.get atomic-reference)))

  IMeta
  (-meta [_] meta)

  IWatchable
  (-notify-watches [this old-val new-val]
    (doseq [[key f] @watches]
      (f key this old-val new-val)))
  (-add-watch [this key f]
    (swap! watches assoc key f))
  (-remove-watch [this key]
    (swap! watches dissoc key))

  IHash
  (-hash [this] (goog/getUid this)))

(defn atom
  ([x]
   (atom x {}))
  ([x & {:keys [meta validator]}]
   (->CasAtom (AtomicReference. (box x)) meta validator
     (->CasAtom (AtomicReference. (box nil)) nil nil nil))))

(defn compare-and-set!
  [a old-val new-val]
  (if (satisfies? ICompareAndSet a)
    (-compare-and-set! a old-val new-val)
    (cljs.core/compare-and-set! a old-val new-val)))
