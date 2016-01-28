;; Copyright (C) 2011, Eduardo Julián. All rights reserved.
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0
;; (http://opensource.org/licenses/eclipse-1.0.php) which can be found
;; in the file epl-v10.html at the root of this distribution.
;;
;; By using this software in any fashion, you are agreeing to be bound
;; by the terms of this license.
;;
;; You must not remove this notice, or any other, from this software.

(ns clj-amazon.core
  "The core functionality shared by other namespaces."
  (:import (java.io ByteArrayInputStream))
  (:require [clj-http.client :as http]
            [clojure.xml :as xml]))


(def UTF-8 "UTF-8")


(defn percent-encode-rfc-3986
  [s encoding]
  (-> (java.net.URLEncoder/encode (str s) encoding)
    (.replace "+" "%20")
    (.replace "*" "%2A")
    (.replace "%7E" "~")))


(defn timestamp
  []
  (-> (doto (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'.000Z'")
        (.setTimeZone (java.util.TimeZone/getTimeZone "GMT")))
    (.format (.getTime (java.util.Calendar/getInstance)))))


(defn canonicalize [sorted-map encoding]
  (if (empty? sorted-map)
    ""
    (->> sorted-map
      (map (fn [[k v]] (if v (str (percent-encode-rfc-3986 k encoding) "=" (percent-encode-rfc-3986 v encoding)))))
      (filter (comp not nil?))
      (interpose "&")
      (apply str))))


(defn- parse-xml
  [xml]
  (xml/parse (ByteArrayInputStream. (.getBytes xml UTF-8))))


(defn fetch-url
  [url]
  (-> url http/get :body parse-xml))


(defn encode-url
  [url encoding]
  (if url (java.net.URLEncoder/encode url encoding)))


(defn assoc+
  ([m k v]
   (let [item (get m k)]
     (if k
       (cond (nil? item) (assoc m k v)
             (vector? item) (assoc m k (conj item v))
             :else (assoc m k [item v]))
       m)))
  ([m k v & kvs] (apply assoc+ (assoc+ m k v) kvs)))


(defn _bool->str
  [bool] (if bool "True" "False"))


(defn _str->sym
  [string]
  (-> (reduce #(if (Character/isUpperCase %2) (str %1 "-" (Character/toLowerCase %2)) (str %1 %2)) "" string)
    (.substring 1) symbol ))


(defn _extract-strs
  [strs]
  (map #(if (list? %) (second %) %) strs))


(defn _extract-vars
  [strs]
  (map _str->sym (_extract-strs strs)))
