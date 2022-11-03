# org-rwtodd.argparse.core

This is just a simple clojure library for parsing command-line args.

It's very similar to `tools.cli`, but more minimal (120 lines or so,
and no regexes needed for parsing).  It does still understand many
syntaxes, like multiple short options concatenated (`-tvf`) and using
equals with long options (`--file=toaster.txt`).  It also stops
looking for switches after a `--` argument.  So you don't give up much
by going with this library.

## Example

```clojure
(def arg-spec {
   :times   (ap/int-param "NUM" "Number of times (0-5)"
                :default 5    :validator #(<= 0 % 5))
   :verbose (ap/counter-param "Verbosity Level" :short \v)
   :help    (ap/flag-param "Get Help" :short \?)
   })

...
(let [options (ap/parse arg-spec args)] ...)

...
(println "Options:\n" (ap/help-text arg-spec))
```

## Tips

- You always have to have a long name.  Any flag can have a short name also.
- `parse` throws `IllegalArgumentException`s when it finds input it doesn't
  like.
- Any args which don't look like switches are returned with key
  `:free-args` in the order they were found.
