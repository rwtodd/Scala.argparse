(ns org-rwtodd.argparse.core)

;;(def example-spec
;;  {
;;   :times   [\t "Number of times (0-5)" { :arg "NUM" :default 5
;;                                         :parser #(Integer/parseInt %)
;;                                         :validator #(<= 0 % 5) } ]
;;   :verbose [\v "Verbosity Level" { :default 0 :update-fn inc } ]
;;   :help    [\? "Get Help" ]
;;   })

(defn- switch-has-args?
  [spec sw]
  (if-let [switch-spec (get (sw spec) 2)]
    (:arg switch-spec)
    false))

(defn- short-has-args?
  [sspec spec char]
  (if-let [sw (get sspec char)]
    (switch-has-args? spec sw)
    false))

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
          (if (switch-has-args? spec kw)
            (list kw kwarg)
            (throw (IllegalArgumentException. (str arg " does not take args!")))))))

    ;; parse -abc short switches
    (.startsWith arg "-")
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
                           (comp (map (fn [[ln sp]] [ln (:default (get sp 2))]))
                                 (filter second))
                           spec)
           ;; any args prior to "--" are expanded to their long form
           args      (let [[parsable unparsed] (split-with (partial not= "--") args)
                           short-args          (into {}
                                                     (comp (map (fn [[ln sp]] [(first sp) ln]))
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
          (let [entry (get spec a1)
                parms (get entry 2)]
            (when (nil? entry)
              (throw (IllegalArgumentException. (str "Unrecognized argument: " a1))))
            (if (:arg parms)
              ;; ok, a2 should be an argument, which should be a string
              (do
                (when (not (string? a2))
                  (throw (IllegalArgumentException. (str "No Argument given for " a1))))
                (let [parsed (try ((or (:parser parms) identity) a2)
                                  (catch Exception e (throw (IllegalArgumentException.
                                                             (str "Could not parse <" a2
                                                                  "> as an arg for switch " a1)))))
                      valid? ((or (:validator parms) yes) parsed)]
                  (when (not valid?)
                    (throw (IllegalArgumentException. (str "Value <" a2 "> is an invalid setting for " a1))))
                  (recur (if-let [uf (:update-fn parms)]
                           (update settings a1 (partial uf parsed))
                           (assoc settings a1 parsed))
                         (drop 2 args))))
              ;; this parameter takes no arguments...
              (recur (update settings a1 (or (:update-fn parms) yes))
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
    (doseq [switch (sort (keys spec))]
      (let [entry (get spec switch)
            short (get entry 0)
            desc  (get entry 1)
            argnm (get (get entry 2) :arg)]
        (.append sb "--") (.append sb (.substring (str switch) 1))
        (when short (.append sb "|-") (.append sb short))
        (when argnm (.append sb "  ") (.append sb argnm))
        (.append sb "\n    ")
        (.append sb desc)
        (.append sb \newline)))
    (.toString sb)))
