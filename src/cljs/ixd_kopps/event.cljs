(ns ixd-kopps.event
  (:require [re-frame.core :refer [reg-event-db]]))

(def lecture
  {:kind :lecture
   :duration 2
   :groups 1
   :simultaneous true
   :teachers #{}
   :comment ""
   :own-room false})

(def seminar
  {:kind :seminar
   :duration 2
   :groups 2
   :simultaneous true
   :teachers #{}
   :comment ""
   :own-room false})

(def schedule
  [[lecture lecture seminar]
   []
   [lecture seminar]
   [lecture lecture seminar]
   [lecture]
   [lecture lecture seminar]
   [seminar]
   [lecture lecture]
   []
   []
   [seminar seminar]
   [lecture seminar]
   [lecture seminar]
   [lecture seminar]
   [lecture seminar]
   [lecture seminar]
   [lecture seminar]
   [lecture seminar]
   [lecture seminar]
   [lecture seminar]])

(def course-instance
  {:start {:week-num 3 :year 2017 :term "VT17"}
   :ladok-num 1
   :duration 20
   :schedule schedule})

(def course
  {:instances [course-instance]})

(reg-event-db :initialize
  (fn [db _]
    {:selected-course "DD2628"
     :selected-instance 0
     :courses {"DD2628" course}}))
