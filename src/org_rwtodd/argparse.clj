(ns org-rwtodd.argparse)

;;(def example-spec
;;  {
;;   :times   (int-param "NUM" "Number of times (0-5)"
;;                :default 5    :validator #(<= 0 % 5))
;;   :verbose (counter-param "Verbosity Level" :short \v)
;;   :help    (flag-param "Get Help" :short \?)
;;   })
;;
;; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
;; Keys:
;;   :doc STRING -- the documentation for the parameter
;;   :short CHAR -- the character for the short-version of the parameter
;;   :arg STRING -- the name of the argument (also this is what tells the parser to expect an argument)
;;         :default -- the default value of the param, prior to parsing the cmdline 
;;         :parser  -- the function to parse the argument
;;         :validator -- a predicate to validate the parsed argument
;;         :update-fn -- the function to use to fuse an argument with whatever we already had
;; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

;; predefined param helpers ~~~~~
(defn flag-param
  "Defines a flag parameter, which is set to `true` when given."
  [doc & keys]
  (merge { :doc doc } (apply hash-map keys)))
   
(defn int-param
  "Defines an integer parameter taking arg ARG with documentation DOC"
  [arg doc & keys]
  (merge { :doc doc, :arg arg, :parser #(Integer/parseInt %) } (apply hash-map keys)))

(defn counter-param
  "Defines a parameter that counts how many times it is found in the cmdline args."
  [doc & keys]
  (merge { :doc doc, :default 0, :update-fn inc } (apply hash-map keys)))
;; END predefined param helpers ~~~~~

(defn- param-has-args?
  "look up :arg for param P in the parameter spec SPEC. If the key is there, it takes args."
  [spec p]
  (:arg (get spec p)))

(defn- short-has-args?
  "determine if a param given by short name CHAR takes args"
  [sspec spec char]
  (param-has-args? spec (get sspec char)))

(defn- expand-arg
  "Expand an argument into potentially multiple arguments"
  [spec shspec arg]
  (cond
    ;; parse --longSwitch=arg
    (.startsWith arg "--")
    (let [eqidx (.indexOf arg (int \=))]
      (if (== eqidx -1)
        (list (keyword (.substring arg 2)))
        (let [kw    (keyword (.substring arg 2 eqidx))
              kwarg (.substring arg (inc eqidx))]
          (if (param-has-args? spec kw)
            (list kw kwarg)
            (throw (IllegalArgumentException. (str arg " does not take args!")))))))

    ;; parse -abc short switches, as long as it isn't just '-' by itself
    (and (.startsWith arg "-") (> (.length arg) 1))
    (let [no-args  (map #(get shspec % %)
                        (take-while (fn [x] (not (short-has-args? shspec spec x)))
                                    (.substring arg 1)))
          remains (.substring arg (inc (count no-args)))]
      (concat no-args
              (case (.length remains)
                0 nil
                1 [(get shspec (.charAt remains 0))]
                (list (get shspec (.charAt remains 0))
                      (.substring remains 1)))))

    ;; parse anything else
    :else (list arg)))

;; spec--
;; { :long-form [short-form desc { :default df :parser ps :validator vd :update-fn f }] }
(defn parse
  "Parse `args` according to `spec`"
  [spec args]
  (let [yes (constantly true)]
    ;; loop over the arguments, accumulating settings
    (loop [settings  (into {:free-args []}
                           (comp (map (fn [[long-name param]] [long-name (:default param)]))
                                 (filter second))
                           spec)
           ;; any args prior to "--" are expanded to their long form
           args      (let [[parsable unparsed] (split-with (partial not= "--") args)
                           short-args          (into {}
                                                     (comp (map (fn [[long-name param]] [(:short param) long-name]))
                                                           (filter first))
                                                     spec)]
                       (concat
                        (mapcat (partial expand-arg spec short-args) parsable)
                        (drop 1 unparsed)))]
      (let [[a1 a2] args]
        (cond
          ;; are we done?
          (nil? a1) settings

          ;; do we have a switch?
          (keyword? a1)
          (let [param (get spec a1)]
            (when-not param
              (throw (IllegalArgumentException. (str "Unrecognized argument: " a1))))
            (if (:arg param)
              ;; ok, a2 should be an argument, which should be a string
              (do
                (when (not (string? a2))
                  (throw (IllegalArgumentException. (str "No Argument given for " a1))))
                (let [parsed (try ((or (:parser param) identity) a2)
                                  (catch Exception e (throw (IllegalArgumentException.
                                                             (str "Could not parse <" a2
                                                                  "> as an arg for switch " a1)))))
                      valid? ((or (:validator param) yes) parsed)]
                  (when-not valid?
                    (throw (IllegalArgumentException. (str "Value <" a2 "> is an invalid setting for " a1))))
                  (recur (if-let [uf (:update-fn param)]
                           (update settings a1 (partial uf parsed))
                           (assoc settings a1 parsed))
                         (drop 2 args))))
              ;; this parameter takes no arguments...
              (recur (update settings a1 (or (:update-fn param) yes))
                     (rest args))))

          ;; if the next arg is a character, it wasn't found as a short option. complain!
          (char? a1)
          (throw (IllegalArgumentException. (str "Unknown short option -" a1)))

          ;; if the next arg is a string, just collect it...
          (string? a1)
          (recur (update settings :free-args conj a1)
                 (rest args)))))))

(defn help-text
  "Generate help text for the arguments in `spec`"
  [spec]
  (let [^StringBuilder sb (StringBuilder.)]
    (doseq [param (sort (keys spec))]
      (let [{:keys [short doc arg]} (get spec param)]
        (.append sb "--") (.append sb (.substring (str param) 1))
        (when short (.append sb "|-") (.append sb short))
        (when arg   (.append sb "  ") (.append sb arg))
        (.append sb "\n    ")
        (.append sb doc)
        (.append sb \newline)))
    (.toString sb)))
