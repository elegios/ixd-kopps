(ns ixd-kopps.app-test
  (:require-macros [cljs.test :refer [deftest testing is]])
  (:require [cljs.test :as t]
            [ixd-kopps.app :as app]))

(deftest test-arithmetic []
  (is (= (+ 0.1 0.2) 0.3) "Something foul is a float."))
