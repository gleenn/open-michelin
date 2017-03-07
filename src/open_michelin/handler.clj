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
(def available-restaurant-names (set ["Luce - InterContinental San Francisco" "Roka Akor - San Francisco" "5A5 Steak Lounge" "Rich Table" "Spruce" "Coqueta" "The Cavalier" "Roka Akor - San Francisco" "Trou Normand" "Park Tavern" "Bob's Steak & Chop House - San Francisco" "Ristorante Milano" "Waterbar" "PABU" "Black Cat" "Hog and Rocks" "Luce - InterContinental San Francisco" "Barcha" "Blue Plate" "The Rotunda at Neiman Marcus - San Francisco" "Foreign Cinema" "A16 - San Francisco" "Catch" "Big 4 Restaurant" "Delfina Restaurant" "Aatxe" "Fringale" "Lolinda" "Venticello" "Cassava" "RN74" "Trace" "BIX" "Anchor and Hope" "Grill at the St. Regis" "Central Kitchen" "Chaya Brasserie" "Blowfish Sushi - SF" "The Waterfront Restaurant and Cafe" "Town Hall" "North Beach Restaurant" "Prospect" "Garibaldi's on Presidio" "Jardiniere" "Baonecci Ristorante" "Gaspar Brasserie" "La Mar Cebicheria Peruana" "Bobo's" "Mission Beach Cafe" "25 Lusk" "Greens Restaurant" "District - San Francisco" "Fior D'Italia" "Alioto's" "Cesario's" "Pane e Vino Trattoria" "Sam’s Grill & Seafood Restaurant" "The Perennial" "Alfred's Steakhouse" "TamashiSoul Sushi Bar" "Ruth's Chris Steak House - San Francisco" "Mochica" "Daily Grill - San Francisco" "Le Colonial - SF" "Laurel Court Restaurant & Bar - Fairmont San Francisco" "Hayes Street Grill" "Fogo de Chao Brazilian Steakhouse - San Francisco" "Farina" "John's Grill" "FOG CITY" "One Market Restaurant" "Old Bus Tavern" "Roy's - San Francisco" "Sens Restaurant" "Ideale" "Osso Steakhouse" "L'Ottavo - San Francisco" "Palio d'Asti" "McCormick & Kuleto's Seafood Restaurant" "Rosa Mexicano - San Francisco" "Tartine Manufactory" "Zingari Ristorante" "Chart House Restaurant - San Francisco" "Gold Mirror Italian Restaurant" "August (1) Five" "OneUP Restaurant & Lounge at Grand Hyatt San Francisco" "Urban Tavern" "Level III" "New Delhi Restaurant" "Fondue Cowboy" "750 Restaurant & Bar" "Rooh" "The Keystone" "Taste on Ellis"]))
(defn handle-intersection [req]
  (layout
    [:div.left.third
     [:ul
      [:li [:h1 "Michelin Star Restaurants"]]
      (for [name (sort michelin-star-restaurant-names)]
        [:li name])]]
    [:div.left.third
     [:ul
      [:li [:h1 "Available on 3/13"]]
      (for [name (sort available-restaurant-names)]
        [:li name])]]
    [:div.left.third
     [:ul
      [:li [:h1 "Intersection"]]
      (for [name (sort (vec (set/intersection available-restaurant-names michelin-star-restaurant-names)))]
        [:li name])]]
    ))

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
