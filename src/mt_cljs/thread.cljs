(ns mt-cljs.thread)

(def thread (.type js/Java "java.lang.Thread"))

(defn sleep [millis]
  (.sleep thread millis))

(defn create [f]
  (new thread f))

(defn start [thread]
  (.start thread))
