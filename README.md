# mt-cljs

Multithreaded ClojureScript

ClojureScript doesn't provide support for multithreaded programming, but this is an experiment to see what might work.

## Example Use

First, start up Nashorn

```shell
$ clj -m cljs.main -re nashorn
```

### Atoms

ClojureScript's atoms are not syncrhonized. Conisder the `swap!` implementation, which behaves roughly as if written like this:

```clojure
(defn swap! [a f]
  (reset! a (f @a)))
```

Let's say two threads `swap!` using `inc`—two threads could increment the same starting value, and sinstead of incrementing 
the atom value by two, it could end up only incremented by one. You can actually see this if you try with multiple threads
in Nashorn.

This illustrates using a lock-free atom implementaion that is based on Java's 
`java.util.concurrent.atomic.AtomicReference`:


```clojure
(require '[mt-cljs.cas-atom :as cas-atom]
         '[mt-cljs.thread :refer [Thread] :as Thread])

(def initial-value {:v 0
                    :a {:v 0
                        :b {:v 0
                            :c {:v 0
                                :d {:v 0
                                    :e {:v 0
                                        :f {:v 0}}}}}}})

(def a (atom initial-value))

(defn worker []
  (swap! a update-in (concat (take (rand-int 7) [:a :b :c :d :e :f]) [:v]) inc)
  (Thread/sleep 10)
  (recur))
  
(dotimes [_ 200]
  (.start (Thread. worker)))
```

This will start up a couple hundred threads `swap!`ing on a CAS-based atom.

If you evaluate `@a` to see the atom's value, you will see something like this

```clojure
{:v 73998, :a {:v 73539, :b {:v 73408, :c {:v 73566, :d {:v 73212, :e {:v 73474, :f {:v 73427}}}}}}}
```

### Lazy Sequence

It is possible to exhibit the lack of synchronization in ClojureScript's lazy sequences 
if you have multiple threads race realizing a given sequence:

```clojure
(def rands (repeatedly rand))

(dotimes [_ 2] 
  (.start (Thread. #(prn (apply + (take 100 rands))))))
```

This can print, for example:

```clojure
48.46728060604748
47.952525424098596
```

But, it is possible to devise a (globally) synchronized `LazySeq` implementation using 
a single `java.util.concurrent.locks.ReentrantLock`. The `mt-cljs.lazy-seq` namespace sufficiently
monkey-patches the `cljs.core` implementation so that the above works. With

```clojure
(require 'mt-cljs.lazy-seq)
```

the above will instead print something like 

```clojure
52.279921036293175
52.279921036293175
```