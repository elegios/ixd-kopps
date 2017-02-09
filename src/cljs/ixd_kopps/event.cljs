(ns ixd-kopps.event
  (:require [re-frame.core :refer [reg-event-db]]))

(reg-event-db :initialize
  (fn [db _]
    {}))
