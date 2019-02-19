
(ns app.updater.template
  (:require [app.schema :as schema]
            [bisection-key.core :as bisection]
            [bisection-key.util :refer [key-append key-after key-prepend key-before]]
            [app.util :refer [path-with-children]]))

(defn after-markup [db op-data sid op-id op-time]
  (let [template-id (:template-id op-data), focused-path (:path op-data)]
    (if (empty? focused-path)
      db
      (update-in
       db
       (concat [:templates template-id :markup] (path-with-children (butlast focused-path)))
       (fn [children]
         (let [next-key (key-after children (last focused-path))]
           (assoc children next-key (merge schema/markup {:id next-key, :type :box}))))))))

(defn append-markup [db op-data sid op-id op-time]
  (let [template-id (:template-id op-data), focused-path (:path op-data)]
    (update-in
     db
     (concat [:templates template-id :markup] (path-with-children focused-path))
     (fn [children]
       (let [next-key (key-append children)]
         (assoc children next-key (merge schema/markup {:id next-key, :type :box})))))))

(defn before-markup [db op-data sid op-id op-time]
  (let [template-id (:template-id op-data), focused-path (:path op-data)]
    (if (empty? focused-path)
      db
      (update-in
       db
       (concat [:templates template-id :markup] (path-with-children (butlast focused-path)))
       (fn [children]
         (let [next-key (key-before children (last focused-path))]
           (assoc children next-key (merge schema/markup {:id next-key, :type :box}))))))))

(defn create-mock [db op-data sid op-id op-time]
  (let [template-id (:template-id op-data)
        text (:text op-data)
        new-mock (merge schema/mock {:id op-id, :name text})]
    (assoc-in db [:templates template-id :mocks op-id] new-mock)))

(defn create-template [db op-data sid op-id op-time]
  (let [markup-id "system"
        base-markup (merge schema/markup {:id markup-id, :type :box, :layout :row})
        new-template (merge schema/template {:id op-id, :name op-data, :markup base-markup})]
    (assoc-in db [:templates op-id] new-template)))

(defn iter-merge-children [container picked-id xs]
  (if (empty? xs)
    container
    (let [next-container (assoc container picked-id (first xs))]
      (recur next-container (key-after next-container picked-id) (rest xs)))))

(defn prepend-markup [db op-data sid op-id op-time]
  (let [template-id (:template-id op-data), focused-path (:path op-data)]
    (update-in
     db
     (concat [:templates template-id :markup] (path-with-children focused-path))
     (fn [children]
       (let [next-key (key-prepend children)]
         (assoc children next-key (merge schema/markup {:id next-key, :type :box})))))))

(defn remove-markup [db op-data sid op-id op-time]
  (let [template-id (:template-id op-data), path (:path op-data)]
    (if (empty? path)
      db
      (update-in
       db
       (concat [:templates template-id :markup] (path-with-children (butlast path)))
       (fn [children] (dissoc children (last path)))))))

(defn remove-mock [db op-data sid op-id op-time]
  (let [template-id (:template-id op-data), mock-id (:mock-id op-data)]
    (update-in db [:templates template-id :mocks] (fn [mocks] (dissoc mocks mock-id)))))

(defn remove-template [db op-data sid op-id op-time]
  (update db :templates (fn [templates] (dissoc templates op-data))))

(defn rename-mock [db op-data sid op-id op-time]
  (assoc-in
   db
   [:templates (:template-id op-data) :mocks (:mock-id op-data) :name]
   (:text op-data)))

(defn rename-template [db op-data sid op-id op-time]
  (let [id (:id op-data), new-name (:name op-data)]
    (update-in db [:templates id] (fn [template] (assoc template :name new-name)))))

(defn set-node-layout [db op-data sid op-id op-time]
  (let [template-id (:template-id op-data), path (:path op-data), layout (:layout op-data)]
    (assoc-in
     db
     (concat
      [:templates template-id :markup]
      (interleave (repeat :children) path)
      [:layout])
     layout)))

(defn set-node-style [db op-data sid op-id op-time]
  (let [template-id (:template-id op-data)
        path (:path op-data)
        property (:property op-data)
        value (:value op-data)]
    (assoc-in
     db
     (concat
      [:templates template-id :markup]
      (interleave (repeat :children) path)
      [:style property])
     value)))

(defn set-node-type [db op-data sid op-id op-time]
  (let [template-id (:template-id op-data), path (:path op-data), new-type (:type op-data)]
    (assoc-in
     db
     (concat [:templates template-id :markup] (interleave (repeat :children) path) [:type])
     new-type)))

(defn spread-markup [db op-data sid op-id op-time]
  (let [template-id (:template-id op-data), focused-path (:path op-data)]
    (if (empty? focused-path)
      db
      (update-in
       db
       (concat [:templates template-id :markup] (path-with-children (butlast focused-path)))
       (fn [container]
         (let [last-id (last focused-path)
               target (get-in container last-id)
               children (:children target)]
           (if (empty? children)
             (dissoc container last-id)
             (iter-merge-children container last-id (vals children)))))))))

(defn update-mock [db op-data sid op-id op-time]
  (let [template-id (:template-id op-data), mock-id (:mock-id op-data), data (:data op-data)]
    (assoc-in db [:templates template-id :mocks mock-id :data] data)))

(defn update-node-attrs [db op-data sid op-id op-time]
  (let [template-id (:template-id op-data)
        path (:path op-data)
        change-type (:type op-data)
        change-key (:key op-data)
        value (:value op-data)]
    (update-in
     db
     (concat [:templates template-id :markup] (interleave (repeat :children) path) [:attrs])
     (fn [attrs]
       (case change-type
         :remove (dissoc attrs change-key)
         :set (assoc attrs change-key value)
         (do (println "Unknown op" change-type) attrs))))))

(defn update-node-preset [db op-data sid op-id op-time]
  (let [template-id (:template-id op-data)
        path (:path op-data)
        op-kind (:op op-data)
        value (:value op-data)]
    (update-in
     db
     (concat
      [:templates template-id :markup]
      (interleave (repeat :children) path)
      [:presets])
     (fn [presets] (if (= op-kind :add) (conj presets value) (disj presets value))))))

(defn update-node-props [db op-data sid op-id op-time]
  (let [template-id (:template-id op-data)
        path (:path op-data)
        change-type (:type op-data)
        change-key (:key op-data)
        value (:value op-data)]
    (update-in
     db
     (concat [:templates template-id :markup] (interleave (repeat :children) path) [:props])
     (fn [props]
       (case change-type
         :remove (dissoc props change-key)
         :set (assoc props change-key value)
         (do (println "Unknown op" change-type) props))))))

(defn update-node-style [db op-data sid op-id op-time]
  (let [template-id (:template-id op-data)
        path (:path op-data)
        change-type (:type op-data)
        change-key (:key op-data)
        value (:value op-data)]
    (update-in
     db
     (concat [:templates template-id :markup] (interleave (repeat :children) path) [:style])
     (fn [style]
       (case change-type
         :remove (dissoc style change-key)
         :set (assoc style change-key value)
         (do (println "Unknown op" change-type) style))))))

(defn use-mock [db op-data sid op-id op-time]
  (assoc-in db [:templates (:template-id op-data) :mock-pointer] (:mock-id op-data)))

(defn wrap-markup [db op-data sid op-id op-time]
  (let [template-id (:template-id op-data), focused-path (:path op-data)]
    (if (empty? focused-path)
      db
      (update-in
       db
       (concat
        [:templates template-id :markup]
        (interleave (repeat :children) focused-path))
       (fn [node]
         (merge schema/markup {:id op-id, :type :box, :children {bisection/mid-id node}}))))))
