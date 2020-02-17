(ns hash-blob-server.data-model
  (:require
   [clojure.java.io :as io]
   [schema.core :as s]
   [schema.utils :as sutil]
   [clj-time.core :as t]
   [clj-time.coerce :as c]
   [digest]))

;; (c/from-long 0)

(def UnixTime-Schema s/Int)
(def HashAlgorithmSha256 "sha256")

(def Base-Schema
  {:id s/Int})

(def UnixTimeEntry-Schema
  {:time_create UnixTime-Schema})

(def MetaDataFile-Schema
  (merge Base-Schema
         UnixTimeEntry-Schema
         {:content_type_id s/Int
          :source_name s/Str
          :user_id s/Int}))

(def HashAlgorithm-Schema
  (merge Base-Schema
         {:name (s/enum HashAlgorithmSha256)}))

(def Sha256Entry-Schema
  (merge Base-Schema
         {:hash #"^[a-zA-Z0-9]{64}"}))

(def BlobEntry-Schema
  (merge Base-Schema
         {:size s/Int}))

(def EntryHash-Schema
  (merge Base-Schema
         {:blob_id s/Int
          :hash_algorithm_id s/Int
          :hash_entry_id s/Int}))

(def LocalFilePathHistoryEntry-Schema
  (merge Base-Schema
         UnixTimeEntry-Schema
         {:entry_hash_id s/Int
          :path s/Str
          :time_verify UnixTime-Schema
          :file_exists s/Bool}))

(def PosixFilePermissionEntry-Schema
  (merge Base-Schema
         {:local_file_path_history_entry_id s/Int
          :rwx s/Int}))

(def PosixPermissionLevel-ALL :all)
(def PosixPermissionLevel-USER :user)
(def PosixPermissionLevel-GROUP :group)

(def PosixPermissionLevel-Schema
  (s/enum
   PosixPermissionLevel-ALL
   PosixPermissionLevel-USER
   PosixPermissionLevel-GROUP))
(def PosixPermissionSet-Schema
  {:r PosixPermissionLevel-Schema
   :w PosixPermissionLevel-Schema
   :x PosixPermissionLevel-Schema})

(defn parse-posix-permission-octal-string [octal-string]
  (->> (seq octal-string)
       (map vector [:r :w :x])))

(let [s "777"]
  (defn octal-string->posix-permission-entry [octal-string]
    (->> (seq octal-string)
         (map (fn [val]
                (->> (Integer/toBinaryString
                      (Integer/parseInt
                       (str val)))
                     (parse-posix-permission-octal-string))))))
  (octal-string->posix-permission-entry s))

;; (parse-posix-permission-octal-string "111")
;; (int->posix-permission-entry 511)

(defn int->posix-permission-entry [permission-entry-int]
  {:pre [(s/validate s/Int permission-entry-int)]
   :post [#(s/validate PosixPermissionSet-Schema %)]}
  (->> [[PosixPermissionLevel-ALL   (mod (quot permission-entry-int 64) 8)]
        [PosixPermissionLevel-GROUP (mod (quot permission-entry-int 8 ) 8)]
        [PosixPermissionLevel-USER  (mod (quot permission-entry-int 1 ) 8)]]
       (map (fn [[level ival]]
              (->> (Integer/toBinaryString ival)
                   (parse-posix-permission-octal-string)
                   (map
                    (fn [[mode bin]]
                      [mode
                       (case bin
                         \1 level
                         \0 nil)]))
                   (filter (fn [[mode level]] level)))))
       (apply concat)
       (group-by first)
       (map (fn [[mode mapping]]
              [mode (map last mapping)]))
       (into {})))

(defn posix-permission-entry-to-int []
  )

(defn get-file-rwx [file]
  (-> file
      (.toURI)
      (java.nio.file.Paths/get)
      ((juxt #(java.nio.file.Files/isReadable %)
             #(java.nio.file.Files/isWritable %)
             #(java.nio.file.Files/isExecutable %)))))

(defn int->rwx [rwx-int]
  (let [base-struct (->> PosixPermissionSet-Schema
                         (map (fn [[k v]]
                                [k false]))
                         (into {}))]
    (->> rwx-int
         (Integer/toBinaryString)
         (seq)
         (map #(= \1 %))
         (take-last 3)
         (map vector [:r :w :x])
         (into base-struct))))

(defn rwx->int [{:keys [r w x]}]
  {:pre [(every? boolean? [r w x])]}
  (->> [r w x]
       (map-indexed
        (fn [power enabled?]
          (* (if enabled?
               1 0)
             (int (Math/pow 2 power)))))
       (apply +)))

(doseq
    [f
     (->> (io/file "/tmp/trash/ankistream")
          (file-seq)
          (remove (fn [f] (.isDirectory f)))
          (take 2))]
    (let [now-millis (-> (t/now)
                         (c/to-long))]
      (-> (merge
           Sha256Entry-Schema
           LocalFilePathHistoryEntry-Schema
           {:hash (-> f
                      (io/as-file)
                      (digest/sha-256))}
           {:entry_hash_id 1}
           {:path (.getPath f)
            :file_exists true
            :rwx (->> f
                      (get-file-rwx)
                      (map vector [:r :w :x])
                      (into {})
                      (rwx->int))}
           {:time_create now-millis
            :time_verify now-millis})
          (clojure.pprint/pprint))))
