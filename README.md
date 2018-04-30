# argparse

This is just a simple scala library for parsing command-line args.  
I think giving each flag a `does` argument with closures avoids 
a lot of cruft I see in other libs (where you have to then ask 
the parser if it got the arguments, and what they were, etc).  

At the bottom of the following snippet, 
the vars `verbosity` and `level` are set 
and ready to use.

## Example

```scala
def main(inargs: Array[String]) = {
  var verbosity = 0
  var level = 0

  val args: Args = new Args(
     new FlagArg("-v","increases verbosity")
           .does { _ => verbosity += 1 },
     new IntArg("-l", "<level> sets the starting level (1-3)")
           .defaultsTo(1).choices(1 to 3).does { level = _ },
     new FlagArg("-help", "shows this help message")
           .does { _ => args.showHelp() }
  )

  val extraArgs = args.parse(inargs)
```

... and the help text describes the arguments formatted roughly like man-pages:

```
OPTIONS
   -v     increases verbosity

   -l <level>
          sets the starting level (1-3)

   -help  shows this help message          
```

To put prolog or epilog text on the help, just turn the Args construction into an
anonymous class and override it:

```scala
lazy val args: Args = new Args(
             ...flags go here...
         ) {
   override def showHelp() = {
      println("stuff")
      super.showHelp()
      println("more stuff")
   }
}
```

