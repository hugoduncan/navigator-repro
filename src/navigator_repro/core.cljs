(ns navigator-repro.core
  "A basic UI for adding, removing and editing items in a list."
  (:require-macros
   [natal-shell.alert-ios :refer [alert]]
   [natal-shell.core :refer [with-error-view]]
   [natal-shell.components
    :refer [image list-view navigator text touchable-highlight view]]
   [natal-shell.data-source :refer [data-source clone-with-rows]]
   [navigator-repro.macros :refer [with-om-vars]])
  (:require [om.next :as om :refer-macros [defui]]))

(set! js/React
      (js/require "react-native/Libraries/react-native/react-native.js"))

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
    (with-error-view
      (let [{:keys [title key] :as obj} (dissoc (om/props this)
                                                :om.next/computed)
            {:keys [nav] :as props} (om/get-computed this)]
        (view {:style {:flex-direction "row"
                       :border-top-width 1
                       :border-color "#000"
                       :background-color "#F00"}}
              (text {} (or title "no title"))
              (touchable-highlight
               {:style {:border-width 1
                        :border-radius 3
                        :border-color "#000"
                        :background-color "#F00"}
                :onPress #(om/transact! this `[(obj/delete ~obj) :objs])}
               (text {:style {:background-color "#F00"}} "Delete")))))))

(def objs-row (om/factory ObjsRowComponent {:keyfn :key}))

(def objs-ds (data-source #js{:rowHasChanged (fn [a b] (!= a b))}))

(defui ObjsViewComponent
  static om/IQuery
  (query [this]
    '[:objs])

  Object
  (render [this]
    (with-error-view
      (let [{:keys [objs] :as props} (om/props this)
            {:keys [nav] :as props} (om/get-computed this)]
        (view
         {:style {:flexDirection "column" :margin 40 :alignItems "center"}}
         (touchable-highlight
          {:style {:backgroundColor "#999" :padding 10 :borderRadius 5}
           :onPress (fn [] (om/transact! this `[(obj/new {}) :objs]))}
          (text
           {:style {:color "white" :textAlign "center" :fontWeight "bold"}}
           "New obj"))

         (list-view
          {:dataSource (clone-with-rows objs-ds (clj->js (or (vals objs) [])))
           :renderRow (fn objs-render-r-row [row section-id row-id]
                        (with-om-vars this
                          (objs-row (om/computed
                                     (js->clj row :keywordize-keys true)
                                     {:nav nav}))))
           :style {:border-width 1 :border-color "#000"}}))))))

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

(def reconciler
  (om/reconciler
   {:state app-state
    :parser (om/parser {:read read :mutate mutate})
    :root-render #(try
                    (.render js/React %1 %2)
                    (catch js/Error e
                      (println "error on render" e)
                      (.log js/console e))
                    )
    :root-unmount #(try
                     (.unmountComponentAtNode js/React %)
                     (catch js/Error e
                      (println "error on unmount" e)
                      (.log js/console e)))}))

(om/add-root! reconciler ObjsViewComponent 1)
