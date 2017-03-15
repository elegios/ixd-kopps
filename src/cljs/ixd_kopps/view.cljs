(ns ixd-kopps.view
  (:require [re-frame.core :as rf :refer [dispatch]]
            [clojure.string :as str]))

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
    [:option {:value :seminar} "Seminar"]
    [:option {:value :exercise} "Exercise"]
    [:option {:value :lab} "Lab"]
    [:option {:value :exam} "Exam"]]))

(defn week-moment
  [week-num num]
  (let [{:keys [kind duration groups comment own-room teachers number groups-edit]} (query :week-moment week-num num)]
    [:div.moment
     [:div.kind-icon {:class kind} (str number)]
     [:div.kind (moment-kind-select week-num num kind)]
     [:div.duration [:input {:type :number :value duration
                             :on-change #(dispatch [:update-duration week-num num (-> % .-target .-value int)])}]]
     [:div.groups [:input {:type :number :value (reduce + groups)
                           :on-change #(dispatch [:update-group-count week-num num (-> % .-target .-value int)])}]]
     (if (and (not groups-edit) (= groups [1]))
       [:div.simultaneous]
       [:div.simultaneous (when-not groups-edit {:on-click #(do
                                                              (-> % .-currentTarget (.getElementsByTagName "input") (aget 0) .focus)
                                                              (dispatch [:begin-group-edit week-num num]))
                                                 :class "editable"
                                                 :title "Ändra vilka grupper som schemaläggs samtidigt"})
         [:span.sim-groups
          (for [g groups]
            [:span.sim-group (str g)])]
         [:div.editor {:class (when groups-edit "editing")}
          [:div.help-text "Dra grupper tillsammans för att de ska schemaläggas samtidigt..."]
          [:div.groups-display {:on-drag-over #(.preventDefault %)
                                :on-drop (fn [e]
                                           (.preventDefault e)
                                           (.stopPropagation e)
                                           (dispatch [:move-group week-num num (-> e .-dataTransfer (.getData "origin-group") int) nil]))}
           (for [[i group] (zipmap (range) groups)]
             [:div.group-display {:on-drag-over #(.preventDefault %)
                                  :on-drop (fn [e]
                                             (.preventDefault e)
                                             (.stopPropagation e)
                                             (dispatch [:move-group week-num num (-> e .-dataTransfer (.getData "origin-group") int) i]))}
              (repeat group [:img {:src "img/group.svg"
                                   :draggable true
                                   :on-drag-start #(-> % .-dataTransfer (.setData "origin-group" i))}])])]
          [:div.help-text "...eller skriv här:"]
          [:input {:class (when (:error groups-edit) "error")
                   :type :text :value (:text groups-edit)
                   :on-change #(dispatch [:update-group-edit (-> % .-target .-value)])}]]])
     [:div.teachers [:input {:type :text :value (str teachers)
                             :on-change #(dispatch [:update-teachers week-num num (-> % .-target .-value)])}]]
     [:div.comment [:input {:type :text :value comment
                            :on-change #(dispatch [:update-comment week-num num (-> % .-target .-value)])}]]
     [:div.own-room [:input {:type :checkbox :checked own-room
                             :on-change #(dispatch [:toggle-own-room week-num num])}]]
     [:div.actions
      [:div.hovered
       [:img {:src "img/add.svg"
              :on-click #(dispatch [:new-moment-at week-num (inc num) :lecture])
              :title "Lägg till nytt moment"}]
       [:img {:src "img/duplicate.svg"
              :on-click #(dispatch [:duplicate-moment week-num num])
              :title "Kopiera moment"}]
       [:img {:src "img/trash.svg"
              :on-click #(dispatch [:remove-moment-at week-num num])
              :title "Ta bort moment"}]]]]))

(defn new-week-moment
  [week-num]
  [:div.moment.new
   [:div.kind-icon.unknown]
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
        {:keys [number num-moments]} (query :week week-num)
        is-removed (query :is-removed week-num)
        is-added (query :is-added week-num)]
    (when is-added
      (dispatch [:mark-added-week-at week-num]))
    [:div.week (when (or is-removed is-added) {:class :minimized})
     [:div.week-header
      [:div.number (str "Vecka " number)
       [:span.course-number (str (inc week-num))]]
      [:div.actions
       [:img {:src "img/add.svg"
              :title (if can-add-weeks "Lägg till vecka" "Schemaplaneringen är full")
              :class (when-not can-add-weeks "disabled")
              :on-click (when can-add-weeks #(dispatch [:new-week-at (inc week-num)]))}]
       [:img {:src "img/duplicate.svg"
              :title (if can-add-weeks "Kopiera vecka" "Schemaplaneringen är full")
              :class (when-not can-add-weeks "disabled")
              :on-click (when can-add-weeks #(dispatch [:duplicate-week week-num]))}]
       [:img {:src "img/trash.svg"
              :on-click (fn []
                           (dispatch [:mark-removed-week-at week-num])
                           (js/setTimeout #(dispatch [:remove-week-at week-num]) 500))
              :title "Ta bort vecka"}]]]
     [:div.week-content
      [:div.header
       [:div.kind-icon]
       [:div.kind "Moment"]
       [:div.duration "Timmar"]
       [:div.groups "Grupper"]
       [:div.simultaneous "Fördelning"]
       [:div.teachers "Lärare"]
       [:div.comment "Kommentar"]
       [:div.own-room "Egen sal"]
       [:div.actions]]
      (if (pos? num-moments)
        (for [num (range num-moments)]
          ^{:key num} [week-moment week-num num])
        [new-week-moment week-num])]]))

(defn summary []
  (let [summary (query :summary)]
    [:div.summary
     [:div.header
      [:div.kind "Moment"]
      [:div.count "Antal moment"]
      [:div.duration "Antal timmar"]]
     (for [[kind {:keys [count total-duration]}] summary]
       ^{:key kind} [:div.summary-row
                     [:div.kind (case kind
                                  :lecture "Lecture"
                                  :seminar "Seminar"
                                  :exercise "Exercise"
                                  :lab "Lab"
                                  :exam "Exam"
                                  :total "Total")]
                     [:div.count count]
                     [:div.duration total-duration]])]))


(defn main []
  (let [num-weeks (query :num-weeks)
        can-add-weeks (query :can-add-weeks)
        ids (query :week-ids)
        {:keys [start-week end-week]} (query :start-end-week)
        editing (query :editing-group)]
    [:div.main
     [:div.cancel-edit (when editing {:class "editing"
                                      :on-click #(dispatch [:end-group-edit])})]
     [:div.weeks
       (for [[num id] (map vector (range num-weeks) ids)]
         ^{:key id} [week num])
       [:div.end-of-schedule {:class (when can-add-weeks "can-add")}
         (if can-add-weeks
           [:div.add-new-week {:on-click #(dispatch [:new-week-at num-weeks])}
            "Lägg till ny vecka"]
           [:div.info-text "Kursen är slut här!"])]
       [:div.beskrivning "Sammanfattning"]
       [summary]]]))
