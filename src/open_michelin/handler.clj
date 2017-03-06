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
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

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

(defn respond [req]
  (let [query (-> req :params :q edn/read-string)]
    (hiccup/html5 [:head]
                  [:body
                   [:div [:h1 "Open Michelin"]
                    [:form {:action "/"}
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
                        ])]
                    ]])))

(defroutes app-routes
           (GET "/" [] respond)
           (route/not-found "Not Found"))

(def app
  (-> app-routes
      wrap-reload
      wrap-refresh
      wrap-logger
      (wrap-defaults site-defaults)
      ))
