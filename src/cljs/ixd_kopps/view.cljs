(ns ixd-kopps.view
  (:require [re-frame.core :as rf :refer [dispatch]]))

(defn query
  [sub & args]
  @(rf/subscribe (apply vector sub args)))

(defn moment-kind-select
  ([] (moment-kind-select nil))
  ([selected]
   [:select {:default-value (if selected selected -1)}
    [:option.default {:value -1} "Nytt moment..."]
    [:option {:value :lecture} "Lecture"]
    [:option {:value :seminar} "Seminar"]]))

(defn week-moment
  [week-num num]
  (let [{:keys [kind duration groups simultaneous comment own-room teachers]}
        (query :week-moment week-num num)]
    [:div.moment
     [:div.kind (moment-kind-select kind)]
     [:div.duration [:input {:type :text :value duration}]]
     [:div.groups [:input {:type :text :value groups}]]
     [:div.simultaneous
      (when (> groups 1)
        [:input {:type :checkbox :checked simultaneous}])]
     [:div.teachers [:input {:type :text :value (str teachers)}]]
     [:div.comment [:input {:type :text :value comment}]]
     [:div.own-room [:input {:type :checkbox :checked own-room}]]
     [:div.actions
      [:div.hovered
       [:span.button "⇅"]
       [:span.button "⊕"]
       [:span.button "✘"]]]]))

(defn week-copy-source []
  [:select {:default-value -1}
    [:option {:value -1} "Kopiera från vecka..."]
    (for [{:keys [week-num num]} (query :copiable-weeks)]
      ^{:key num} [:option {:value num} (str "v" week-num)])])

(defn week
  [week-num]
  (let [{:keys [number num-moments]} (query :week week-num)]
    [:div.week
     [:div.number (str "v" number)]
     [:div.content
      (if (pos? num-moments)
        (for [num (range num-moments)]
          ^{:key num} [week-moment week-num num])
        [:div.empty-week
         (moment-kind-select)
         (week-copy-source)])]]))

(defn main []
  [:div.weeks
    [:div.header
     [:div.number "Vecka"]
     [:div.kind "Moment"]
     [:div.duration "Timmar"]
     [:div.groups "Grupper"]
     [:div.simultaneous "Samtidigt"]
     [:div.teachers "Lärare"]
     [:div.comment "Kommentar"]
     [:div.own-room "Egen sal"]]
    (for [num (range (query :num-weeks))]
      ^{:key num} [week num])])
