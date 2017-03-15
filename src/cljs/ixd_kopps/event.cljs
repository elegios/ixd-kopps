(ns ixd-kopps.event
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [com.rpl.specter :refer [ALL LAST END srange must collect-one] :refer-macros [setval transform path]]
            [clojure.string :as str]))

(def lecture
  {:kind :lecture
   :duration 2
   :groups [1]
   :teachers "Ylva Fernaeus"
   :comment ""
   :own-room false})

(def seminar
  {:kind :seminar
   :duration 2
   :groups [2]
   :teachers ""
   :comment ""
   :own-room false})

(def exercise
  {:kind :exercise
   :duration 2
   :groups [1 1]
   :teachers ""
   :comment ""
   :own-room false})

(def lab
  {:kind :lab
   :duration 4
   :groups [2 3]
   :teachers ""
   :comment ""
   :own-room false})

(def exam
  {:kind :exam
   :duration 5
   :groups [1]
   :teachers ""
   :comment ""
   :own-room false})

(defn default-moment
  [kind]
  (case kind
    :lecture lecture
    :seminar seminar
    :exercise exercise
    :lab lab
    :exam exam))

(def schedule
  (transform [ALL]
             #(vary-meta % assoc :id (rand-int 100000000))
             [[lecture lecture]
              [lecture lab]
              [lecture lecture seminar]
              [lecture lecture exercise]
              [lecture lecture seminar]
              [lecture exercise]
              []
              [lecture lecture exercise]
              [lecture seminar]
              [lecture lecture exercise]
              [lecture seminar]
              [lecture lecture exercise]]))

(def course-instance
  {:start {:week-num 3 :year 2017 :term "VT17"}
   :ladok-num 1
   :duration 12
   :schedule schedule
   :number-of-students 0
   :to-be-removed #{}
   :to-be-added #{}})

(def course
  {:instances [course-instance]})

(reg-event-db :initialize
  (fn [db _]
    {:selected-course "DD2628"
     :selected-instance 0
     :editing-group nil  ; {:week-num int :num int :text string :original [int]}
     :courses {"DD2628" course}}))

(reg-event-db :update-number-of-students
  (fn [db [_ number-of-students]]
    (let [{:keys [selected-course selected-instance]} db]
      (setval [:courses (must selected-course) :instances (must selected-instance) :number-of-students]
              number-of-students
              db))))

(reg-event-db :update-kind
  (fn [db [_ week-num num kind]]
    (let [{:keys [selected-course selected-instance]} db]
      (setval [:courses (must selected-course) :instances (must selected-instance) :schedule (must week-num) (must num) :kind]
              kind
              db))))

(reg-event-db :update-duration
  (fn [db [_ week-num num duration]]
    (let [{:keys [selected-course selected-instance]} db]
      (setval [:courses (must selected-course) :instances (must selected-instance) :schedule (must week-num) (must num) :duration]
              duration
              db))))

; BUG: will not do the correct thing if a group editor is open at the same time
(reg-event-db :update-group-count
  (fn [db [_ week-num num group-count]]
    (let [{:keys [selected-course selected-instance]} db]
      (transform [:courses (must selected-course) :instances (must selected-instance) :schedule (must week-num) (must num) :groups]
                 (fn [prev]
                   (let [prev-group-count (reduce + prev)
                         num-added (- group-count prev-group-count)]
                     (cond
                       (neg? num-added)
                       (loop [next []
                              [curr & prev] prev
                              remaining group-count]
                         (cond
                           (or (not curr) (= remaining 0)) next
                           (<= curr remaining) (recur (conj next curr) prev (- remaining curr))
                           :default (conj next remaining)))

                       (or (= (count prev) 1)
                           (not-every? #(= % 1) prev))
                       (transform [LAST] #(+ num-added %) prev)

                       :default (setval [END] (repeat num-added 1) prev))))
                 db))))

(reg-event-db :begin-group-edit
  (fn [db [_ week-num num]]
    (let [{:keys [selected-course selected-instance]} db
          original (get-in db [:courses selected-course :instances selected-instance :schedule week-num num :groups])]
      (assoc db :editing-group
        {:week-num week-num
         :num num
         :text (str/join ", " original)
         :error nil
         :original original}))))

(reg-event-db :update-group-edit
  (fn [db [_ new-text]]
    (let [{:keys [selected-course selected-instance editing-group]} db
          {:keys [original week-num num]} editing-group
          groups (seq (map int (re-seq #"\d+" new-text)))
          _ (js/console.log (str "groups: " groups))
          error (when-not groups "Hittar inga siffror")
          groups (or groups original)]
      (->> db
           (setval [:courses (must selected-course) :instances (must selected-instance) :schedule (must week-num) (must num) :groups]
                   groups)
           (setval [:editing-group :text]
                   new-text)
           (setval [:editing-group :error]
                   error)))))

(reg-event-db :move-group
  (fn [db [_ week-num num from-idx to-idx]]
    (let [{:keys [selected-course selected-instance]} db
          group-path (path [:courses (must selected-course) :instances (must selected-instance) :schedule (must week-num) (must num) :groups])]
      (->> db
           (transform [group-path]
                      (fn [prev]
                        (if to-idx
                          (->> prev
                               (transform [(must from-idx)] dec)
                               (transform [(must to-idx)] inc)
                               (filter pos?)
                               vec)
                          (->> prev
                               (transform [(must from-idx)] dec)
                               (setval [END] [1])
                               (filter pos?)
                               vec))))
           (transform [(collect-one group-path) :editing-group :text]
                      (fn [groups _] (str/join ", " groups)))))))

(reg-event-db :end-group-edit
  (fn [db _]
    (dissoc db :editing-group)))

(reg-event-db :update-comment
  (fn [db [_ week-num num comment]]
    (let [{:keys [selected-course selected-instance]} db]
      (setval [:courses (must selected-course) :instances (must selected-instance) :schedule (must week-num) (must num) :comment]
              comment
              db))))

(reg-event-db :update-teachers
  (fn [db [_ week-num num teachers]]
    (let [{:keys [selected-course selected-instance]} db]
      (setval [:courses (must selected-course) :instances (must selected-instance) :schedule (must week-num) (must num) :teachers]
              teachers
              db))))

(reg-event-db :make-simultaneous
  (fn [db [_ week-num num simultaneous]]
    (let [{:keys [selected-course selected-instance]} db]
      (transform [:courses (must selected-course) :instances (must selected-instance) :schedule (must week-num) (must num) :groups]
                 #(let [groups (reduce + %)]
                    (if simultaneous
                      [groups]
                      (vec (repeat groups 1))))
                 db))))

(reg-event-db :toggle-own-room
  (fn [db [_ week-num num]]
    (let [{:keys [selected-course selected-instance]} db]
      (transform [:courses (must selected-course) :instances (must selected-instance) :schedule (must week-num) (must num) :own-room]
                 not
                 db))))

(reg-event-db :new-moment-at
  (fn [db [_ week-num num kind]]
    (let [{:keys [selected-course selected-instance]} db]
      (setval [:courses (must selected-course) :instances (must selected-instance) :schedule (must week-num) (srange num num)]
              [(default-moment kind)]
              db))))

(reg-event-db :duplicate-moment
  (fn [db [_ week-num num]]
    (let [{:keys [selected-course selected-instance]} db]
      (transform [:courses (must selected-course) :instances (must selected-instance) :schedule (must week-num) (collect-one (must num)) (srange (inc num) (inc num))]
                 (fn [prev _] [prev])
                 db))))

(reg-event-db :remove-moment-at
  (fn [db [_ week-num num]]
    (let [{:keys [selected-course selected-instance]} db]
      (setval [:courses (must selected-course) :instances (must selected-instance) :schedule (must week-num) (srange num (inc num))]
              []
              db))))

(reg-event-db :new-week-at
  (fn [db [_ week-num]]
    (let [{:keys [selected-course selected-instance]} db]
      (->> db
           (setval [:courses (must selected-course) :instances (must selected-instance) :schedule (srange week-num week-num)]
                   [(vary-meta [] assoc :id (rand-int 100000000))])
           (transform [:courses (must selected-course) :instances (must selected-instance) :to-be-added]
                      #(conj % week-num))))))

(reg-event-db :duplicate-week
  (fn [db [_ week-num]]
    (let [{:keys [selected-course selected-instance]} db]
      (->> db
           (transform [:courses (must selected-course) :instances (must selected-instance) :schedule (collect-one (must week-num)) (srange (inc week-num) (inc week-num))]
                      (fn [prev _] [(vary-meta prev assoc :id (rand-int 100000000))]))
           (transform [:courses (must selected-course) :instances (must selected-instance) :to-be-added]
                      #(conj % (inc week-num)))))))

(reg-event-db :mark-added-week-at
  (fn [db [_ week-num]]
    (let [{:keys [selected-course selected-instance]} db]
      (transform [:courses (must selected-course) :instances (must selected-instance) :to-be-added]
                 #(disj % week-num)
                 db))))

(reg-event-db :remove-week-at
  (fn [db [_ week-num]]
    (let [{:keys [selected-course selected-instance]} db]
      (->> db
           (setval [:courses (must selected-course) :instances (must selected-instance) :schedule (srange week-num (inc week-num))]
                   [])
           (transform [:courses (must selected-course) :instances (must selected-instance) :to-be-removed]
                      #(disj % week-num))))))

(reg-event-db :mark-removed-week-at
  (fn [db [_ week-num]]
    (let [{:keys [selected-course selected-instance]} db]
      (transform [:courses (must selected-course) :instances (must selected-instance) :to-be-removed]
              #(conj % week-num)
              db))))
