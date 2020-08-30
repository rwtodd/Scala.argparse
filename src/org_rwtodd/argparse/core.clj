(ns org-rwtodd.argparse.core)

(def example-spec
  {
   :times   [\t "Number of times" { :arg "N" :default 5 :parser #(Integer/parseInt %) } ]
   :verbose [\v "Verbosity Level" { :default 0 :update-fn inc } ]
   :help    [\? "Get Help" ]
   })

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
  (let [short-args (into {}
                         (comp (map (fn [[ln sp]] [(first sp) ln]))
                               (filter first))
                         spec)
        defaults   (into {}
                         (comp (map (fn [[ln sp]] [ln (:default (get sp 2))]))
                               (filter second))
                         spec)]
    (let [[parsable unparsed] (split-with (partial not= "--") args)]
      (concat
       (mapcat (partial expand-arg spec short-args) parsable)
       (drop 1 unparsed)))))

