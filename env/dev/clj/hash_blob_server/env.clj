(ns hash-blob-server.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [hash-blob-server.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[hash-blob-server started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[hash-blob-server has shut down successfully]=-"))
   :middleware wrap-dev})
