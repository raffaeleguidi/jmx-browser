(ns jmxappl.util
  (:require [noir.io :as io]
            [markdown.core :as md]))

(defn md->html
  "reads a markdown file from public/md and returns an HTML string"
  [filename]
  (md/md-to-html-string (io/slurp-resource filename)))

(defn sizeOrZero [set] 
   (if (empty? set) 0 (.size set)))

