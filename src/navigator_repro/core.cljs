(ns navigator-repro.core
  "A basic UI for adding, removing and editing items in a list."
  (:require-macros
   [navigator-repro.macros :refer [with-om-vars]])
  (:require
   [goog.dom :as gdom]
   [om.next :as om :refer-macros [defui]]
   [om.dom :as dom]))

(enable-console-print!)

(defonce app-state (atom {:objs {"a" {:key "a" :title "A"}
                                 "b" {:key "b" :title "B"}}}))

(defui ObjsRowComponent
  static om/Ident
  (ident [this {:keys [key]}]
    [:objs key])

  static om/IQuery
  (query [this]
    [:key :title])

  Object
  (render [this]
    (let [{:keys [title key] :as obj} (dissoc (om/props this)
                                              :om.next/computed)
          {:keys [nav] :as props} (om/get-computed this)]
      (dom/div {}
               (dom/p {}
                      (or title "no title")
                      (dom/button
                       #js {:onClick
                            (fn [_]
                              (om/transact! this `[(obj/delete ~obj) :objs]))}
                       (dom/span {} "Delete"))
                      (dom/button
                       #js {:onClick
                            (fn [_]
                              (om/transact! this `[(obj/update ~(update-in obj [:title] str ".")) :objs]))}
                       (dom/span {} "Update")))))))

(def objs-row (om/factory ObjsRowComponent {:keyfn :key}))

(defui ObjsViewComponent
  static om/IQuery
  (query [this]
    '[:objs])

  Object
  (render [this]
    (let [{:keys [objs] :as props} (om/props this)
          {:keys [nav] :as props} (om/get-computed this)]
      (dom/div
       {}
       (dom/button
        #js {:onClick (fn [] (om/transact! this `[(obj/new {}) :objs]))}
        (dom/span {} "New obj"))

       (dom/ul {}
               (for [obj (vals objs)]
                 (dom/li {} (objs-row obj))))))))

(def objs-view (om/factory ObjsViewComponent))

(defmulti read om/dispatch)

(defmethod read :default
  [{:keys [state]} k _]
  (let [st @state]
    (if-let [[_ v] (find st k)]
      {:value v}
      {:value :not-found})))

(defmulti mutate om/dispatch)

(defmethod mutate 'obj/new
  [{:keys [state]} _ _]
  {:action
   (fn []
     (let [k (str (int (rand 100)))
           r {:key k :title (str "Hello " k)}]
       (swap! state assoc-in [:objs k] r)))})

(defmethod mutate 'obj/delete
  [{:keys [state]} _ {:keys [key]}]
  {:action
   (fn []
     (swap! state update-in [:objs] dissoc key))})

(defmethod mutate 'obj/update
  [{:keys [state]} _ {:keys [key] :as obj}]
  {:action
   (fn []
     (swap! state update-in [:objs key] merge obj))})

(def reconciler
  (om/reconciler
   {:state app-state
    :parser (om/parser {:read read :mutate mutate})}))

(om/add-root! reconciler ObjsViewComponent (gdom/getElement "main-app-area"))

(enable-console-print!)
