(ns open-michelin.handler
  (:require [clojure.edn :as edn]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as s]
            [clojure.tools.logging.impl :as log-impl]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [net.cgrand.enlive-html :as html]
            [hiccup.page :as hiccup]
            [hiccup.core :refer [h]]
            [open-michelin.wrap_logger :refer [wrap-logger]]
            [ring.middleware
             [refresh :refer [wrap-refresh]]
             [reload :refer [wrap-reload]]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.set :as set]))

(defn third [seq] (get seq 2))

(defn h2s [h] (with-out-str (pprint h)))

(defn d [val]
  (h (if (map? val)
       (h2s val)
       val)))

(defn michelin-restaurant-names [selector]
  (->> (-> "http://sf.eater.com/maps/michelin-stars-san-francisco-bay-area-2017-map"
           java.net.URL.
           html/html-resource
           (html/select [:.c-mapstack__card]))
       (map :content)
       (filter #(= :h2 (:tag (second %))))
       (map (fn [restaurant-block]
              {:name  (last (:content (second restaurant-block)))
               ;:stars (d (:content (second (:content (get restaurant-block 4)))))
               :stars (-> restaurant-block #(do (prn (nth 2 %)) (nth 2 %)) :content d)
               ;:address (first contents)
               }))
       ;(map (comp s/trim last :content))
       ;(filter #(not (seq (re-seq #"Related Maps" %))))
       ))

(defn layout [& bodies]
  (hiccup/html5 [:head
                 [:style
                  (str ".left { float: left; }"
                       ".right { float: right; }"
                       ".third { width: 33%; }"
                       )
                  ]]
                [:body
                 [:div [:h1 "Open Michelin"]
                  (for [body bodies]
                    body)
                  ]]))

(defn handle-search [req]
  (let [query (-> req :params :q edn/read-string)]
    (layout [:form {:action "/"}
             "Search:"
             [:input {:name "q" :value query :autofocus true}]
             [:input {:type "submit"}]
             ]
            [:br]
            [:div {:class "restaurants"}
             (for [restaurant (vec (michelin-restaurant-names query))]
               [:div {:style "background-color: #eee; margin: 10px; padding: 5px"}
                [:div (:name restaurant)]
                [:div (:stars restaurant)]
                [:div (:address restaurant)]
                ])])))


(def michelin-star-restaurant-names #{"Acquerello" "Adega" "Al's Place" "Aster" "Atelier Crenn" "Auberge du Soleil" "Aziza" "Baumé" "Benu" "Bouchon" "Californios" "Campton Place Restaurant; Bistro & Bar" "Chez TJ" "Coi" "Commis" "Commonwealth" "Farmhouse Inn Restaurant" "Gary Danko" "Hashiri" "jū-ni" "Keiko à Nob Hill" "Kin Khao" "La Toque Restaurant" "Lazy Bear" "Lord Stanley" "Luce" "Madera" "Madrona Manor" "Manresa" "Michael Mina" "Mister Jiu's" "Mosu" "Mourad" "Nico" "Octavia" "OMAKASE" "Plumed Horse" "Quince" "Rasa" "Saison" "Solbar at Solage Calistoga" "Sons & Daughters" "SPQR" "Spruce" "State Bird Provisions" "Sushi Yoshizumi" "Terra Restaurant" "Terrapin Creek Cafe" "The French Laundry" "The Progress" "The Restaurant at Meadowood" "The Village Pub" "Wako" "Wakuriya"})
(defn handle-intersection [req]
  (layout
    [:div.left.third
     [:ul
      (for [name (sort (vec (set/intersection michelin-star-restaurant-names)))]
        [:li name])]]
    [:div.left.third
     "Hi I'm a third"]))

(defroutes app-routes
           (GET "/" [] handle-search)
           (GET "/intersection" [] handle-intersection)
           (route/not-found "Not Found"))

(def app
  (-> app-routes
      wrap-reload
      wrap-refresh
      wrap-logger
      (wrap-defaults site-defaults)
      ))
