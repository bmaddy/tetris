(ns example.hello
  (:require
    [example.crossover.shared :as shared]
    [clojure.set :as s]
    [goog.dom :as dom]
    ;[goog.events.Event :as goog-event]
    [goog.graphics :as g])
  (:require-macros [cljs.core.logic.macros :as m])
  (:use [cljs.core.logic :only [membero]]))

(defn ^:export say-hello []
  (js/alert (shared/make-example-text)))

(defn add-some-numbers [& numbers]
  (apply + numbers))

(defn ^:export logic []
  (m/run* [q]
          (membero q '(:cat :dog :bird :bat :debra))))



;; ---------------------= Constants =------------------------

(def block-size 20)
(def width 10)
(def height 20)
(def stroke (g/Stroke. 1 "black"))
(def panel (g/createGraphics (* width block-size) (* height block-size)))

;; Blocks and Squares
(def piece-structures {:left-knight [[0 0] [1 0] [1 1] [1 2]]
                       :right-knight [[0 2] [0 1] [0 0] [1 0]]
                       :block [[0 0]]
                       :bar [[0 0] [0 1] [0 2] [0 3]]})

(def piece-structures
  (apply s/union (map (fn [piece] (map #(hash-map :type (first piece), :offset %)
                                       (second piece)))
                      piece-structures)))

(def floor (for [x (range 0 width) :let [b (make-block [x height] "black")]] b))

;; ---------------------= Essential State =------------------------
;; The 'static' structure of the system

(def clock (atom 0))
(def pieces (atom #{}))
(def next-id (atom 0))


;; ---------------------= Essential Logic (behavior) =------------------------

;; Functions (pure only)
;; thse can be used in the derived relations below
(defn game-pos-to-canvas-pos [pos]
  (map (partial * block-size) pos))

;(defn extend-block-with-canvas-positions [block]
;  (conj block {:canvas-position (game-pos-to-canvas-pos (:position block))}))

(defn make-id []
  (swap! next-id inc))

;(defn falling? [piece]
;  true)

;(defn at-bottom? [piece]
;  )

(defn select-falling [pieces]
  (s/select #(not (contains? (set (map :id (select-frozen pieces))) (:id %))) pieces))

(defn select-frozen
  ([pieces] (select-frozen pieces floor))
  ([pieces frozen-blocks]
   (if (or (empty? frozen-blocks) (empty? pieces))
     (empty pieces)
     (let [blocks (s/project (get-blocks pieces) [:id :position])
           frozen-blocks (s/rename (s/project frozen-blocks [:id :position]) {:id :frozen-id})]
       (let [found-frozen (s/join
                            (s/select #(not= (:id %) (:frozen-id %))
                                      (s/join
                                        (move-all-down blocks)
                                        frozen-blocks
                                        {:position :position}))
                            pieces
                            {:id :id})]
         (s/union found-frozen (select-frozen pieces found-frozen)))))))

;; Derived Relations - only restrict, project, product, union, intersection, difference, join, and divide

;; Derived Relations - Internal
;; main purpose is to facilitate the definition of other drived relations

(defn get-blocks [pieces]
  (map #(conj % {:position (map + (:position %) (:offset %))})
       (s/join pieces piece-structures {:type :type})))


;; Derived Relations - External
;; these provide information to the users

;(defn blocks-with-canvas-positions [blocks]
;  (map extend-block-with-canvas-positions blocks))

;; Integrity Constraints

;; ---------------------= Accidental State and Control (performance) =------------------------

;; ---------------------= Other (interfacing) =------------------------

;; Feeders - convert input into relational assignments

(defn make-piece [type pos orientation color]
    {:id (make-id) :type type :position pos :orientation (mod orientation 4) :color color})

(defn make-block [pos color]
  {:position pos :color color})

(defn move-down [piece]
  (let [{[x y] :position} piece]
    (conj piece [:position [x (inc y)]])))

(defn move-all-down [pieces]
  (map move-down pieces))

(defn move-falling-down [pieces]
  (s/union (select-frozen pieces) (move-all-down (select-falling pieces))))

(defn draw-frozen []
  (swap! pieces move-falling-down)
  (draw))

(defn inc-clock []
  (swap! clock inc)
  (swap! pieces move-falling-down)
  (draw))

;; Observers - generate output in response to changes in derived values

(defn start-clock []
  (let [timer (goog.Timer. 500)]
    (do (. timer (start))
    ;(do (.start timer)
      (.listen goog.events timer goog.Timer.TICK inc-clock))))

(defn fill-from-color [color]
  (g/SolidFill. color))

(defn draw-block [{[x y] :position color :color}]
  (.drawRect panel
             (* x block-size)
             (* y block-size)
             block-size
             block-size
             stroke
             (fill-from-color color)))

(defn draw-blocks [blocks]
  (do
    (. panel (clear))
    (dorun (map draw-block blocks))))

(defn draw
  "Draws the given items on the panel"
  []
  (draw-blocks (get-blocks @pieces)))

;; ---------------------= MAIN =------------------------
;; Main entry function
(defn ^:export main []
  ;; Tetris stuff
  (.render panel (.getElement goog.dom "main-panel"))

  ;; dummy stuff for testing
  ;(swap! pieces #(conj % (make-piece :right-knight [3 5] 0 "red")))
  ;(swap! pieces #(conj % (make-piece :bar [5 2] 0 "blue")))
  ;(swap! pieces #(conj % (make-piece :block [5 16] 0 "orange")))
  (swap! pieces #(conj % (make-piece :block [5 19] 0 "orange")))
  (swap! pieces #(conj % (make-piece :left-knight [4 10] 0 "green")))

  ;(start-clock)
  (draw))

