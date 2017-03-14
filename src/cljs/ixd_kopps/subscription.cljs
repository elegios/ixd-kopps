(ns ixd-kopps.subscription
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [com.rpl.specter :refer [ALL MAP-VALS] :refer-macros [select transform traverse]]))

(reg-sub :selected-course-instance
  (fn [{:keys [selected-course selected-instance courses]} _]
    (get-in courses [selected-course :instances selected-instance])))

(reg-sub :num-weeks
  (fn [_ _]
    (subscribe [:selected-course-instance]))
  (fn [{:keys [schedule]} _]
    (count schedule)))

(reg-sub :start-end-week
  (fn [_ _]
    (subscribe [:selected-course-instance]))
  (fn [{:keys [schedule start]} _]
    (let [start (:week-num start)]
      {:start-week start
       :end-week (+ start -1 (count schedule))})))

(reg-sub :week-ids
  (fn [_ _]
    (subscribe [:selected-course-instance]))
  (fn [{:keys [schedule]} _]
    (map #(-> % meta :id) schedule)))

(reg-sub :max-weeks
  (fn [_ _]
    (subscribe [:selected-course-instance]))
  (fn [{:keys [duration]} _]
    duration))

(reg-sub :number-of-students
  (fn [_ _]
    (subscribe [:selected-course-instance]))
  (fn [{:keys [number-of-students]} _]
    number-of-students))

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

(reg-sub :is-removed
  (fn [_ _]
    (subscribe [:selected-course-instance]))
  (fn [{:keys [to-be-removed]} [_ num]]
    (contains? to-be-removed num)))

(reg-sub :is-added
  (fn [_ _]
    (subscribe [:selected-course-instance]))
  (fn [{:keys [to-be-added]} [_ num]]
    (contains? to-be-added num)))

(reg-sub :week-moment
  (fn [_ _]
    (subscribe [:selected-course-instance]))
  (fn [instance [_ week-num num]]
    (let [{:keys [kind] :as moment} (get-in instance [:schedule week-num num])]
      (->> (:schedule instance)
           (sequence
             (comp (map-indexed
                     (fn [idx week]
                       (map conj (map-indexed list week) (repeat idx))))
                   cat
                   (filter (fn [[week-idx moment-idx mom]]
                             (and (= kind (:kind mom))
                                  (or (< week-idx week-num)
                                      (and (= week-idx week-num)
                                           (<= moment-idx num))))))))
           count
           (assoc moment :number)))))


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

(reg-sub :summary
  (fn [_ _]
    (subscribe [:selected-course-instance]))
  (fn [{:keys [schedule]} _]
    (let [moments (select [ALL ALL] schedule)
          grouped (group-by :kind moments)
          basic
          (transform [MAP-VALS]
                     (fn [moments] {:count (count moments)
                                    :total-duration (reduce + (traverse [ALL :duration] moments))})
                     grouped)]
      (assoc basic :total
        {:count (reduce + (traverse [MAP-VALS :count] basic))
         :total-duration (reduce + (traverse [MAP-VALS :total-duration] basic))}))))
