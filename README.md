# mt-cljs

Multithreaded ClojureScript

ClojureScript doesn't provide support for multithreaded programming, but this is an experiment to see what might work.

It works on Nashorn, relying on Java for threads, and defines a lock-free atom implementaion that is based on Java's 
`java.util.concurrent.atomic.AtomicReference`.

### Example Use

First, start up Nashorn

```shell
$ clj -m cljs.main -re nashorn
```

Then evaluate these forms

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