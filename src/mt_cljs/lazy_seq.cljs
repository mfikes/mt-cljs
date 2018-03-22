(ns mt-cljs.lazy-seq)

(def ReentrantLock (.type js/Java "java.util.concurrent.locks.ReentrantLock"))

(def global-lock (ReentrantLock.))

(let [orig (.. cljs.core/LazySeq -prototype -sval)]
  (set! (.. cljs.core/LazySeq -prototype -sval)
    (fn []
      (try
        (.lock global-lock)
        (.call orig (js-this))
        (finally
          (.unlock global-lock))))))
