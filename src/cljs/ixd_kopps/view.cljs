(ns ixd-kopps.view
  (:require [re-frame.core :as rf :refer [dispatch]]))

(defn query
  [sub & args]
  @(rf/subscribe sub (apply vector sub args)))

(defn main []
  [:div "Hello World"])
