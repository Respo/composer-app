
(ns composer.comp.overflow
  (:require [hsl.core :refer [hsl]]
            [composer.schema :as schema]
            [respo-ui.core :as ui]
            [respo.core :refer [defcomp list-> <> span div button input]]
            [respo.comp.space :refer [=<]]
            [composer.config :as config]
            [composer.util :refer [neaten-templates]]
            [composer.core :refer [render-markup]]
            [fuzzy-filter.core :refer [parse-by-word]]
            [feather.core :refer [comp-icon]]
            [clojure.string :as string]))

(defcomp
 comp-overview
 (states templates focuses settings)
 (let [tmpls (neaten-templates templates)
       cursor (:cursor states)
       state (or (:data states) {:filter ""})]
   (div
    {:style (merge ui/flex ui/column {:overflow :auto, :background-color (hsl 0 0 94)})}
    (div
     {:style (merge
              ui/row-middle
              {:padding "8px 200px 8px 144px", :border-bottom "1px solid #ddd"})}
     (input
      {:style ui/input,
       :placeholder "filter...",
       :value (:filter state),
       :on-input (fn [e d!] (d! cursor (assoc state :filter (:value e))))})
     (=< 8 nil)
     (if-not (string/blank? (:filter state))
       (comp-icon
        :delete
        {:font-size 18, :color (hsl 200 80 70), :cursor :pointer}
        (fn [e d!] (d! cursor (assoc state :filter ""))))))
    (list->
     {:style (merge ui/flex {:overflow :auto, :padding "8px 16px 160px 16px"})}
     (->> templates
          (filter
           (fn [[k template]] (:matches? (parse-by-word (:name template) (:filter state)))))
          (sort-by (fn [[k template]] (:sort-key template)))
          (map
           (fn [[k template]]
             [k
              (let [style-container (merge
                                     ui/column
                                     {:background-color (hsl 0 0 100),
                                      :border "1px solid #ddd",
                                      :min-width (or (:width template) 240),
                                      :min-height (or (:height template) 60),
                                      :position :relative})]
                (div
                 {:style (merge ui/row {:margin "16px 0px"})}
                 (div
                  {:style (merge ui/column {:min-width 120, :max-width 240})}
                  (<> (:name template) {:font-family ui/font-fancy, :font-size 20})
                  (let [active-names (->> focuses
                                          (map last)
                                          (filter
                                           (fn [info]
                                             (= k (get-in info [:focus :template-id]))))
                                          (map (fn [info] (get-in info [:user :name])))
                                          (string/join ", "))]
                    (<>
                     active-names
                     {:word-break :break-all,
                      :font-size 12,
                      :font-family ui/font-fancy,
                      :line-height "16px",
                      :color (hsl 0 0 70)})))
                 (list->
                  {:style (merge ui/flex {})}
                  (->> (or (:mocks template) {"fake-id" {:data nil, :state nil}})
                       (map
                        (fn [[k mock]]
                          [k
                           (div
                            {:style (merge
                                     ui/row
                                     {:display :inline-flex,
                                      :margin-right 32,
                                      :padding 8,
                                      :vertical-align :top})}
                            (div
                             {:style style-container}
                             (render-markup
                              (:markup template)
                              {:data (:data mock),
                               :templates tmpls,
                               :level 0,
                               :hide-popup? true,
                               :template-name (:name template),
                               :state-path [],
                               :states (:state mock),
                               :presets (:presets settings)}
                              (fn [d! op param options] (println op param (pr-str options)))))
                            (div
                             {:style {:margin-left 8,
                                      :color (hsl 0 0 70),
                                      :font-size 13,
                                      :font-family ui/font-fancy}}
                             (<> (:name mock))))]))))))])))))))
