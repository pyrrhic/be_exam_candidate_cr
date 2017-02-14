(ns scoir.core
  (:gen-class)
  (:require [clojure-csv.core :as csv]
            [clojure.data.json :as json]
            [hawk.core :as hawk])
  (:import (clojure.lang PersistentQueue)
           (org.apache.commons.io FilenameUtils)))

;validation error messages
(defn m-not-empty [field]
  (str field " can not be empty."))

(defn m-is-string-int [field]
  (str field " must be an integer."))

(defn m-equal-digits [field num-digits]
  (str field " must be " num-digits " digits."))

(defn m-max-length [field num-digits]
  (str field " can not be greater than " num-digits " characters."))

(defn m-phone-format [field]
  (str field " must follow the format ###-###-####"))

;input validators
(defn v-is-string-int [s]
  (try (Integer/parseInt s)
       (catch NumberFormatException npe nil)))

(defn v-equal-digits?-fn [data num-digits]
  (== (count data) num-digits))

(defn v-max-length?-fn [data max-length]
  (<= (count data) max-length))

(defn error-msgs [data preds-err-msgs]
  (let [paired-preds-msgs (partition 2 preds-err-msgs)]
    (remove nil? (map #(if ((first %) data) nil (second %))
                      paired-preds-msgs))))

(def csv-header-names
  {:id "INTERNAL_ID"
   :first "FIRST_NAME"
   :middle "MIDDLE_NAME"
   :last "LAST_NAME"
   :phone "PHONE_NUM"})

(defn all-error-msgs [csv-map]
  (flatten
    (remove empty? [(error-msgs (:id csv-map) [#(v-equal-digits?-fn % 8) (m-equal-digits (:id csv-header-names) 8)
                                               v-is-string-int (m-is-string-int (:id csv-header-names))
                                               not-empty (m-not-empty (:id csv-header-names))])
                    (error-msgs (:first csv-map) [#(v-max-length?-fn % 15) (m-max-length (:first csv-header-names) 15)
                                                  not-empty (m-not-empty (:first csv-header-names))])
                    (error-msgs (:middle csv-map) [#(v-max-length?-fn % 15) (m-max-length (:middle csv-header-names) 15)])
                    (error-msgs (:last csv-map) [#(v-max-length?-fn % 15) (m-max-length (:last csv-header-names) 15)
                                                 not-empty (m-not-empty (:last csv-header-names))])
                    (error-msgs (:phone csv-map) [#(re-matches #"\d{3}-\d{3}-\d{4}" %) (m-phone-format (:phone csv-header-names))
                                                  not-empty (m-not-empty (:phone csv-header-names))])])))

(defn csv-row->person-info [row]
  (if (empty? row)
    {:id     ""
     :first  ""
     :middle ""
     :last   ""
     :phone  ""}
    {:id     (first row)
     :first  (second row)
     :middle (nth row 2)
     :last   (nth row 3)
     :phone  (nth row 4)}))

(defn formatted-person-info [person-info]
  (assoc {} :id (Integer/parseInt (:id person-info))
            :name (as-> {} n
                        (assoc n :first (:first person-info))
                        (if (empty? (:middle person-info))
                          n
                          (assoc n :middle (:middle person-info)))
                        (assoc n :last (:last person-info)))
            :phone (:phone person-info)))

(defn file-cleanup! [input output error]
  (let [in-file (clojure.java.io/file input)
        out-file (clojure.java.io/file output)
        err-file (clojure.java.io/file error)]
    (clojure.java.io/delete-file in-file)
    (when (zero? (.length out-file)) (clojure.java.io/delete-file out-file))
    (when (zero? (.length err-file)) (clojure.java.io/delete-file err-file))))

(defn write-err-msgs! [err-writer err-msgs]
  (doseq [err err-msgs]
    (doto err-writer
      (.write err)
      (.newLine))))

(defn read-parse [csv-row i wrote-out]
  (let [person-info (csv-row->person-info csv-row)
        error-msgs (all-error-msgs person-info)]
    (if (empty? error-msgs)
      {:output (let [prefix (if wrote-out "," "[")]
                 (str prefix (json/write-str (formatted-person-info person-info))))}
      {:errors (for [err error-msgs]
                 (str "LINE NUMBER: " i ", " "ERROR MSG: " err))})))

(defn read-parse-write! [input-csv out-writer err-writer]
  (loop [csv-rows input-csv
         i 1                                                ;0 represents header.
         wrote-out false]
    (if (empty? csv-rows)
      (when wrote-out (.write out-writer "]"))
      (let [result (read-parse (first csv-rows) i wrote-out)
            output (:output result)
            errors (:errors result)]
        (if output
          (.write out-writer output)
          (write-err-msgs! err-writer errors))
        (recur (rest csv-rows) (inc i) (if (or wrote-out output) true false))))))

(defn csv->json! [in-path out-path err-path file-name]
  (let [input-name (str in-path file-name ".csv")
        output-name (str out-path file-name ".json")
        error-name (str err-path file-name ".txt")]
    (with-open [input-rdr (clojure.java.io/reader input-name)
                out-writer (clojure.java.io/writer output-name)
                err-writer (clojure.java.io/writer error-name)]
      (read-parse-write! (rest (csv/parse-csv input-rdr))
                         out-writer
                         err-writer))
    (file-cleanup! input-name
                   output-name
                   error-name)))

(def added-files-queue (atom PersistentQueue/EMPTY))

(defn dequeue! [queue]
  (loop []
    (let [q     @queue
          value (peek q)
          rest-q    (pop q)]
      (if (compare-and-set! queue q rest-q)
        value
        (recur)))))

(defn start-watch [input-dir]
  (hawk/watch! [{:paths   [input-dir]
                 :filter  (fn [_ e] (= (:kind e) :create))
                 :handler (fn [_ e]
                            (swap! added-files-queue #(conj % (:file e))))}]))

(def processed-files #{})

(defn start
  ([]
    (start "./resources/input_directory/"
           "./resources/output_directory/"
           "./resources/error_directory/"))
  ([input-dir output-dir error-dir]
   (start-watch input-dir)
   (loop []
     (let [file-name (let [file (dequeue! added-files-queue)]
                       (if (and (some? file)
                                (= "csv" (FilenameUtils/getExtension (.getName file)))
                                (not (contains? processed-files (.getName file))))
                         (do (alter-var-root (var processed-files) #(conj % (.getName file)))
                             (FilenameUtils/removeExtension (.getName file)))
                         nil))]
       (when file-name
         (do
           (println (str "converting csv to json for " file-name))
           (csv->json! input-dir
                       output-dir
                       error-dir
                       file-name)))
       (recur)))))

(defn format-args [path-args]
  (as-> path-args $
        (map #(.replace % "\\" "/") $)
        (map #(if (not= \/ (.charAt % (dec (.length %))))
               (str % "/")
               %)
             $)))

(defn -main [& args]
  (if (nil? args)
    (start)
    (apply start (format-args args))))

