(ns open-michelin.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.string :as s]
            [clojure.pprint :refer [pprint]]
            [clojure.edn :as edn]
            [net.cgrand.enlive-html :as html]
            [hiccup.page :as hiccup]
            [hiccup.core :refer [h]]
            [ring.middleware
             [refresh :refer [wrap-refresh]]
             [reload :refer [wrap-reload]]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defn michelin-restaurant-names [selector]
  (->> (-> "http://sf.eater.com/maps/michelin-stars-san-francisco-bay-area-2017-map"
           java.net.URL.
           html/html-resource
           (html/select [:.c-mapstack__card :h2]))
       (map (comp s/trim last :content))
       (filter #(not (seq (re-seq #"Related Maps" %))))
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
                    [:ul
                     (for [restaurant (vec (michelin-restaurant-names query))]
                       [:li restaurant])]
                   ]])))

(defroutes app-routes
           (GET "/" [] respond)
           (route/not-found "Not Found"))

(def app
  (-> app-routes
      wrap-reload
      wrap-refresh
      (wrap-defaults site-defaults)))
