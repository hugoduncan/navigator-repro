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
                                 "b" {:key "b" :title "B"}}
                          :obj/focused nil}))


(def ^{:dynamic true :private true} *nav-render*
  "Flag to suppress navigator re-renders from outside om when pushing/popping."
  true)

(defn nav-push [nav route]
  (binding [*nav-render* false]
    (.push nav route)))

(defn nav-pop [nav]
  (binding [*nav-render* false]
    (.pop nav)))

(defui ObjComponent
  static om/Ident
  (ident [this {:keys [key]}]
    [:objs (or key ::not-found)])

  static om/IQuery
  (query [this]
    [:key :title])

  Object
  (render [this]
    (with-error-view "ObjComponent"
      (let [{:keys [key title] :as obj} (om/props this)
            nav (om/get-computed this :nav)]
        (view
         {:style {:flexDirection "column" :margin 40 :alignItems "center"}}
         (text {} (or title "no title"))
         (touchable-highlight
          {:style {:backgroundColor "#999" :padding 10 :borderRadius 5}
           :onPress (fn [] (nav-pop nav))}
          (text {:style {:color "white" :textAlign "center" :fontWeight "bold"}}
                "Back")))))))

(def obj-comp (om/factory ObjComponent {:key-fn :key}))


(defui FocusedObjComponent
  static om/IQuery
  (query [this]
    [{:obj/focused (om/get-query ObjComponent)}])

  Object
  (render [this]
    (with-error-view "FocusedObjComponent"
      (let [{:keys [obj/focused] :as obj} (om/props this)
            nav (om/get-computed this :nav)]
        (obj-comp (om/computed (or focused {}) {:nav nav}))))))

(def focused-obj-comp (om/factory FocusedObjComponent))

(defui ObjsRowComponent
  static om/Ident
  (ident [this {:keys [key]}]
    [:objs (or key ::not-found)])

  static om/IQuery
  (query [this]
    [:key :title])

  Object
  (render [this]
    (with-error-view "ObjsRowComponent"
      (let [{:keys [title key] :as obj} (dissoc (om/props this)
                                                :om.next/computed)
            {:keys [nav] :as props} (om/get-computed this)]
        (when (not= :not-found key)
          (view {:style {:flex-direction "row"
                         :border-top-width 1
                         :border-color "#000"
                         :background-color "#F00"}}
                [(touchable-highlight
                  {:style {}
                   :onPress (fn []
                              (nav-push
                               nav
                               #js{:component focused-obj-comp
                                   :name "Obj"
                                   :props-fn (fn [props]
                                               (select-keys props
                                                            [:obj/focused]))})
                              (om/transact! this `[(obj/focus ~obj)]))}
                  (text {} (or title "no title")))
                 (touchable-highlight
                  {:style {:border-width 1
                           :border-radius 3
                           :border-color "#000"
                           :background-color "#F00"}
                   :onPress #(om/transact! this `[(obj/delete ~obj)])}
                  (text {:style {:background-color "#F00"}} "Delete"))]))))))

(def objs-row (om/factory ObjsRowComponent {:key-fn :key}))

(def objs-ds
  (React.ListView.DataSource.
   #js{:rowHasChanged (fn rds-has-changed [a b] true)}))

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
           :onPress (fn []
                      (om/transact! this `[(obj/new {})])
                      (nav-push
                       nav
                       #js{:component focused-obj-comp
                           :name "Obj"
                           :props-fn (fn [props]
                                       (select-keys props [:obj/focused]))}))}
          (text
           {:style {:color "white" :textAlign "center" :fontWeight "bold"}}
           "New obj"))

         (list-view
          {:dataSource (clone-with-rows objs-ds (clj->js (vals objs)))
           :renderRow (fn objs-render-r-row [row section-id row-id]
                        (with-om-vars this
                          (objs-row (om/computed
                                     (js->clj row :keywordize-keys true)
                                     {:nav nav}))))
           :style {:border-width 1 :border-color "#000"}}))))))

(def objs-view (om/factory ObjsViewComponent))

(defui MainViewComponent
  Object
  (render [this]
    (with-error-view
      (navigator
       {:initialRoute {:component objs-view}
        :renderScene (fn [route nav]
                       (when *nav-render*
                         (let [{:keys [component props-fn] :as route}
                               (js->clj route :keywordize-keys true)
                               props (om/props this)]
                           (when component
                             (component (om/computed
                                         ((or props-fn identity) props)
                                         {:nav nav}))))))
        :style {:flex 1}}))))

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
       (swap! state (fn [state]
                      (-> state
                          (assoc-in [:objs k] r)
                          (assoc :obj/focused r))))))})

(defmethod mutate 'obj/delete
  [{:keys [state]} _ {:keys [key]}]
  {:action
   (fn []
     (swap! state update-in [:objs] dissoc key))})


(defmethod mutate 'obj/focus
  [{:keys [state]} _ {:keys [key] :as obj}]
  {:action
   (fn []
     (swap! state assoc :obj/focused obj))})

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

(om/add-root! reconciler MainViewComponent 1)
