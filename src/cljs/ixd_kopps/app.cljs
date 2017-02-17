(ns ixd-kopps.app
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [dispatch-sync]]
            [ixd-kopps.view :as view]
            [ixd-kopps.subscription :as subscription]
            [ixd-kopps.event :as event]))

(enable-console-print!)

(defn init []
  (dispatch-sync [:initialize])
  (reagent/render-component [view/main]
                            (.getElementById js/document "container")))
