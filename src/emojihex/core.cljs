(ns emojihex.core
  (:require [ajax.core :as ajax]
            [reagent.core :as r]))

(enable-console-print!)

(defonce data (r/atom false))

(ajax/GET "emoji.json"
  {:keywords? true
   :response-format :json
   :handler (fn [d]
              (reset! data
                      (dissoc
                       (->> d
                            (filter :category)
                            (group-by :category))
                       "Flags")))})

(defonce hex-state
  (->> nil (repeat 3) vec (repeat 3) vec r/atom))

(defn randomize [ev]
  (.preventDefault ev)
  (reset! hex-state
          (vec
           (for [i (range 3)]
             (vec
              (for [j (range 3)]
                (let [cat (rand-nth (keys @data))]
                  (-> (rand-nth (get @data cat)) :emoji))))))))

(defonce selecting-panel (r/atom nil))

(defn emoji-select []
  (let [current-panel (r/atom (first (keys @data)))]
    (fn []
      [:div#select
       [:div
        [:select
         {:value @current-panel
          :on-change (fn [ev]
                       (reset! current-panel
                               (.. ev -target -value)))}
         (doall
          (for [[category _] @data]
            ^{:key category}
            [:option
             {:value category}
             (name category)]))]]
       [:ul#select-panel
        (doall
         (for [emoji-data (get @data @current-panel)]
           ^{:key emoji-data}
           [:li
            {:on-click
             (fn [ev]
               (.preventDefault ev)
               (swap! hex-state
                      assoc-in @selecting-panel (:emoji emoji-data))
               (reset! selecting-panel nil))}
            (:emoji emoji-data)]))]])))

(defn emoji-hex []
  [:div#emoji-hex
   [:h2 "Emoji Hexxxx"]
   [:div#table-wrapper
    [:table
     [:tbody
      (map-indexed
       (fn [i row]
         ^{:key i}
         [:tr
          (map-indexed
           (fn [j v]
             ^{:key j}
             [:td
              {:class (when (= [i j] @selecting-panel)
                        "active")
               :on-click (fn [ev]
                           (.preventDefault ev)
                           (reset! selecting-panel [i j]))}
              v])
           row)])
       @hex-state)
      [:tr
       [:td {:col-span 3}

        [:button {:on-click randomize} "RANDOM"]]]]]]

   (when (and @data @selecting-panel)
     [emoji-select])])

(defn app []
  [:div
   (if @data
     [emoji-hex]
     [:h1 "loading"])])

(defn init []
  (r/render [app]
            (js/document.getElementById "app")))
