(ns ixd-kopps.subscription
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub :selected-course-instance
  (fn [{:keys [selected-course selected-instance courses]} _]
    (get-in courses [selected-course :instances selected-instance])))

(reg-sub :num-weeks
  (fn [_ _]
    (subscribe [:selected-course-instance]))
  (fn [{:keys [duration]} _]
    duration))

(reg-sub :week
  (fn [_ _]
    (subscribe [:selected-course-instance]))
  (fn [{:keys [schedule start]} [_ num]]
    (let [week-num (+ num (:week-num start))
          num-moments (count (schedule num))]
      {:number week-num
       :num-moments num-moments})))

(reg-sub :week-moment
  (fn [_ _]
    (subscribe [:selected-course-instance]))
  (fn [instance [_ week-num num]]
    (get-in instance [:schedule week-num num])))
