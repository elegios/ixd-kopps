(ns ixd-kopps.subscription
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub :selected-course-instance
  (fn [{:keys [selected-course selected-instance courses]} _]
    (get-in courses [selected-course :instances selected-instance])))

(reg-sub :num-weeks
  (fn [_ _]
    (subscribe [:selected-course-instance]))
  (fn [{:keys [schedule]} _]
    (count schedule)))

(reg-sub :max-weeks
  (fn [_ _]
    (subscribe [:selected-course-instance]))
  (fn [{:keys [duration]} _]
    duration))

(reg-sub :can-add-weeks
  (fn [_ _]
    [(subscribe [:num-weeks])
     (subscribe [:max-weeks])])
  (fn [[num max] _]
    (< num max)))

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

(reg-sub :copiable-weeks
  (fn [_ _]
    (subscribe [:selected-course-instance]))
  (fn [{:keys [schedule start]} _]
    (let [start-week-num (:week-num start)]
      (keep-indexed
        (fn [idx moments]
          (when (not-empty moments)
             {:week-num (+ start-week-num idx)
              :num idx}))
        schedule))))
