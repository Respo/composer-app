
(ns composer.comp.container
  (:require [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo.core :refer [defcomp <> div span >> button]]
            [respo.comp.inspect :refer [comp-inspect]]
            [respo.comp.space :refer [=<]]
            [composer.comp.navigation :refer [comp-navigation]]
            [composer.comp.profile :refer [comp-profile]]
            [composer.comp.login :refer [comp-login]]
            [respo-message.comp.messages :refer [comp-messages]]
            [cumulo-reel.comp.reel :refer [comp-reel]]
            [composer.config :refer [dev?]]
            [composer.schema :as schema]
            [composer.config :as config]
            [composer.comp.workspace :refer [comp-workspace]]
            [composer.comp.preview :refer [comp-preview]]
            [composer.comp.overflow :refer [comp-overview]]
            [composer.comp.settings :refer [comp-settings]]
            [composer.comp.emulate :refer [comp-emulate]]))

(defcomp
 comp-offline
 ()
 (div
  {:style (merge
           ui/global
           ui/fullscreen
           ui/column-dispersive
           {:background-color (:theme config/site)})}
  (div {:style {:height 0}})
  (div
   {:style {:background-image (str "url(" (:icon config/site) ")"),
            :width 128,
            :height 128,
            :background-size :contain}})
  (div
   {:style {:cursor :pointer, :line-height "32px"},
    :on-click (fn [e d!] (d! :effect/connect nil))}
   (<> "No connection..." {:font-family ui/font-fancy, :font-size 24}))))

(defcomp
 comp-status-color
 (color)
 (div
  {:style (let [size 24]
     {:width size,
      :height size,
      :position :absolute,
      :bottom 60,
      :left 8,
      :background-color color,
      :border-radius "50%",
      :opacity 0.6,
      :pointer-events :none})}))

(defcomp
 comp-container
 (states store)
 (let [state (:data states)
       session (:session store)
       router (:router store)
       settings (:settings store)
       router-data (:data router)
       templates (:templates store)
       focus-to (:focus-to session)
       focuses (:focuses store)]
   (cond
     (nil? store) (comp-offline)
     (= :emulate (:name router)) (comp-emulate templates (get-in session [:router :data]))
     :else
       (div
        {:style (merge ui/global ui/fullscreen ui/column)}
        (comp-navigation
         (:logged-in? store)
         (:count store)
         router
         (:templates-modified? store))
        (if (:logged-in? store)
          (case (:name router)
            :home (comp-workspace (>> states :workspace) templates settings focus-to focuses)
            :preview
              (comp-preview
               (>> states :preview)
               templates
               focus-to
               (:shadows? session)
               focuses
               settings)
            :overview (comp-overview (>> states :overview) templates focuses settings)
            :profile (comp-profile (:user store) (:data router))
            :settings (comp-settings (>> states :settings) settings (:data router))
            (<> router))
          (comp-login states))
        (comp-status-color (:color store))
        (when dev? (comp-inspect "Store" store {:bottom 0, :left 0, :max-width "100%"}))
        (comp-messages
         (get-in store [:session :messages])
         {}
         (fn [info d!] (d! :session/remove-message info)))
        (when dev? (comp-reel (:reel-length store) {:bottom 60}))))))

(def style-body {:padding "8px 16px"})
