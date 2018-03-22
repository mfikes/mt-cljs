(ns mt-cljs.thread)

(def Thread (.type js/Java "java.lang.Thread"))

(defn sleep [millis]
  (.sleep Thread millis))
