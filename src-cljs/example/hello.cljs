(ns example.hello
  (:require
    [example.crossover.shared :as shared]
    [clojure.set :as s]
    [goog.dom :as dom]
    [goog.events.KeyCodes :as key-codes]
    [goog.events.KeyHandler :as key-handler]
    [goog.graphics :as g]))


;; ---------------------= Constants =------------------------

(def block-size 20)
(def width 10)
(def height 20)
(def stroke (g/Stroke. 1 "black"))
(def panel (g/createGraphics (* width block-size) (* height block-size)))

;; Blocks and Squares
(def piece-shapes {:left-knight [[0 0] [1 0] [1 1] [1 2]]
                       :right-knight [[0 2] [0 1] [0 0] [1 0]]
                       :block [[0 0]]
                       :bar [[0 0] [0 1] [0 2] [0 3]]})

(def piece-structures
  (apply s/union (map (fn [piece] (map #(hash-map :type (first piece), :offset %)
                                       (second piece)))
                      piece-shapes)))

;; ---------------------= Essential State =------------------------
;; The 'static' structure of the system

;(def clock (atom 0))
(def pieces (atom #{}))
(def next-id (atom 0))
(def paused (atom false))


;; ---------------------= Essential Logic (behavior) =------------------------

;; Functions (pure only)
;; these can be used in the derived relations below
(defn game-pos-to-canvas-pos [pos]
  (map (partial * block-size) pos))

(declare make-id)
(defn make-piece [type pos orientation color]
    {:id (make-id) :type type :position pos :orientation (mod orientation 4) :color color})

(defn make-block [pos color]
  {:position pos :color color})

(defn move-down [piece]
  (let [{[x y] :position} piece]
    (conj piece [:position [x (inc y)]])))

(defn move-left [piece]
  (update-in piece [:position 0] dec))

(defn move-right [piece]
  (update-in piece [:position 0] inc))

(defn rotate-cw [piece]
  (update-in piece [:orientation] inc))

(defn get-blocks [pieces]
  (map #(conj % {:position (map + (:position %) (:offset %))})
       (s/join pieces piece-structures {:type :type})))

(def floor (for [x (range 0 width)] (make-block [x height] "black")))

;; Derived Relations - only restrict, project, product, union, intersection, difference, join, and divide

;; Derived Relations - Internal
;; main purpose is to facilitate the definition of other drived relations

(defn get-pieces [blocks]
  (s/project
    (s/join (s/project blocks [:id])
            @pieces
            {:id :id})
    [:id :type :position :orientation :color]))

(defn select-overlapping
  "Finds the items in set a that overlap items in set b"
  [a b]
  (s/project
    (s/select #(not= (:id %) (:b-id %))
              (s/join
                a
                (s/rename b {:id :b-id})
                {:position :position}))
    [:id]))

(defn select-colliding
  "Finds and returns the pieces that are colliding with frozen-blocks"
  [pieces frozen-blocks]
  (let [found-frozen-ids (select-overlapping (get-blocks (map move-down pieces))
                                             frozen-blocks)]
    (s/join pieces found-frozen-ids {:id :id})))

(defn select-frozen
  "Selects the pieces that are being held up by the given blocks"
  ([pieces] (select-frozen pieces floor))
  ([pieces frozen-blocks]
   (if (or (empty? frozen-blocks) (empty? pieces))
     (empty pieces)
     (let [found-frozen (select-colliding pieces frozen-blocks)]
       (get-pieces (s/union found-frozen (select-frozen pieces (get-blocks found-frozen))))))))

(defn select-falling
  "Select only falling pieces"
  [pieces]
  (s/difference (set pieces) (select-frozen pieces)))

;; Derived Relations - External
;; these provide information to the users

(defn change-falling [pieces f]
  (s/union (select-frozen pieces) (map f (select-falling pieces))))

(defn move-falling-down
  "Returns a new set of pieces with all the falling pieces moved down one space"
  [pieces]
  (change-falling pieces move-down))

(defn all-blocks
  "Convert our pieces to blocks"
  []
  (get-blocks @pieces))

;; Integrity Constraints

;; ---------------------= Accidental State and Control (performance) =------------------------

;; ---------------------= Other (interfacing) =------------------------

;; Feeders - convert input into relational assignments -- i.e. cause changes to the essential state

(defn make-id []
  (swap! next-id inc))

(defn inc-clock []
  (if (not @paused) (swap! pieces move-falling-down)))

(defn reset-game
  "Resets the game to it's initial state"
  []
    (reset! pieces #{})
    (reset! next-id 0)

    ;; dummy stuff for testing
    (swap! pieces #(conj % (make-piece :right-knight [3 5] 0 "red")))
    (swap! pieces #(conj % (make-piece :bar [5 2] 1 "blue")))
    (swap! pieces #(conj % (make-piece :block [5 16] 0 "orange")))
    (swap! pieces #(conj % (make-piece :block [5 19] 0 "orange")))
    (swap! pieces #(conj % (make-piece :left-knight [4 10] 0 "green"))))

(def key-event-handlers
  {key-codes/P #(swap! paused not)
   key-codes/LEFT (fn [] (swap! pieces #(change-falling % move-left)))
   key-codes/RIGHT (fn [] (swap! pieces #(change-falling % move-right)))
   key-codes/UP (fn [] (swap! pieces #(change-falling % rotate-cw)))})

(defn handle-key-event [e]
  (let [handler (or (key-event-handlers (.-keyCode e))
                    identity)]
    (handler)))

;; Observers - generate output in response to changes in derived values

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
  (draw-blocks (all-blocks)))

(defn start-clock []
  (let [timer (goog.Timer. 500)]
    (do (. timer (start))
      (.listen goog.events timer goog.Timer.TICK #(do (inc-clock) (draw))))))

(defn listen-for-events []
  (clojure.browser.event/listen (goog.events.KeyHandler. js/document.body)
                                "key"
                                #(do
                                   (handle-key-event %)
                                   (draw))))

;; ---------------------= MAIN =------------------------
;; Main entry function
(defn ^:export main []
  (.render panel (.getElement goog.dom "main-panel"))

  (reset-game)
  (listen-for-events)
  (start-clock)

  (draw)
  
  (example.repl.connect))


