(ns ixd-kopps.view
  (:require [re-frame.core :as rf :refer [dispatch]]))

(defn query
  [sub & args]
  @(rf/subscribe (apply vector sub args)))

(defn moment-kind-select
  ([week-num] (moment-kind-select week-num nil nil))
  ([week-num num selected]
   [:select {:value (if selected selected -1)
             :on-change (if num
                          #(dispatch [:update-kind week-num num (-> % .-target .-value keyword)])
                          #(dispatch [:new-moment-at week-num 0 (-> % .-target .-value keyword)]))}
    [:option.default {:value -1} ""]
    [:option {:value :lecture} "Lecture"]
    [:option {:value :seminar} "Seminar"]]))

(defn week-moment
  [week-num num]
  (let [{:keys [kind duration groups simultaneous comment own-room teachers]} (query :week-moment week-num num)]
    [:div.moment
     [:div.kind (moment-kind-select week-num num kind)]
     [:div.duration [:input {:type :number :value duration
                             :on-change #(dispatch [:update-duration week-num num (-> % .-target .-value)])}]]
     [:div.groups [:input {:type :number :value groups
                           :on-change #(dispatch [:update-groups week-num num (-> % .-target .-value)])}]]
     [:div.simultaneous
      (when (> groups 1)
        [:input {:type :checkbox :checked simultaneous
                 :on-change #(dispatch [:toggle-simultaneous week-num num])}])]
     [:div.teachers [:input {:type :text :value (str teachers)}]]
     [:div.comment [:input {:type :text :value comment
                            :on-change #(dispatch [:update-comment week-num num (-> % .-target .-value)])}]]
     [:div.own-room [:input {:type :checkbox :checked own-room
                             :on-change #(dispatch [:toggle-own-room week-num num])}]]
     [:div.actions
      [:div.hovered
       [:img {:src "img/add.svg"
              :on-click #(dispatch [:new-moment-at week-num (inc num) :lecture])}]
       [:img {:src "img/duplicate.svg"
              :on-click #(dispatch [:duplicate-moment week-num num])}]
       [:img {:src "img/trash.svg"
              :on-click #(dispatch [:remove-moment-at week-num num])}]]]]))

(defn new-week-moment
  [week-num]
  [:div.moment.new
   [:div.kind (moment-kind-select week-num)]
   [:div.duration [:input {:disabled true :type :number :value 0}]]
   [:div.groups [:input {:disabled true :type :number :value 0}]]
   [:div.simultaneous]
   [:div.teachers [:input {:disabled true :type :text :value ""}]]
   [:div.comment [:input {:disabled true :type :text :value ""}]]
   [:div.own-room [:input {:disabled true :type :checkbox :checked false}]]
   [:div.actions]])

(defn week-copy-source []
  [:select {:default-value -1}
    [:option {:value -1} "Kopiera från vecka..."]
    (for [{:keys [week-num num]} (query :copiable-weeks)]
      ^{:key num} [:option {:value num} (str "v" week-num)])])

(defn week
  [week-num]
  (let [can-add-weeks (query :can-add-weeks)
        {:keys [number num-moments]} (query :week week-num)]
    [:div.week
     [:div.week-header
      [:div.number (str "Vecka " number)]
      [:div.actions
       [:img {:src "img/add.svg"
              :title (when-not can-add-weeks "Schemaplaneringen är full")
              :class (when-not can-add-weeks "disabled")
              :on-click (when can-add-weeks #(dispatch [:new-week-at (inc week-num)]))}]
       [:img {:src "img/duplicate.svg"
              :title (when-not can-add-weeks "Schemaplaneringen är full")
              :class (when-not can-add-weeks "disabled")
              :on-click (when can-add-weeks #(dispatch [:duplicate-week week-num]))}]
       [:img {:src "img/trash.svg"
              :on-click #(dispatch [:remove-week-at week-num])}]]]
     [:div.content
      [:div.header
       [:div.kind "Moment"]
       [:div.duration "Timmar"]
       [:div.groups "Grupper"]
       [:div.simultaneous "Samtidigt"]
       [:div.teachers "Lärare"]
       [:div.comment "Kommentar"]
       [:div.own-room "Egen sal"]
       [:div.actions]]
      (if (pos? num-moments)
        (for [num (range num-moments)]
          ^{:key num} [week-moment week-num num])
        [new-week-moment week-num])]]))

(defn main []
  (let [num-weeks (query :num-weeks)
        can-add-weeks (query :can-add-weeks)]
    [:div.main
     [:div.weeks
       (for [num (range num-weeks)]
         ^{:key num} [week num])
       [:div.end-of-schedule
         (if can-add-weeks
           [:div.add-new-week {:on-click #(dispatch [:new-week-at num-weeks])}
            "Lägg till ny vecka"]
           [:div.info-text "Kursen är slut här!"])]]]))
