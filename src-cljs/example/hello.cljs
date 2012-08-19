(ns example.hello
  (:require
    [example.crossover.shared :as shared])
  (:require-macros [cljs.core.logic.macros :as m])
  (:use [cljs.core.logic :only [membero]]))

(defn ^:export say-hello []
  (js/alert (shared/make-example-text)))

(defn add-some-numbers [& numbers]
  (apply + numbers))

(defn ^:export logic []
  (m/run* [q]
          (membero q '(:cat :dog :bird :bat :debra))))

