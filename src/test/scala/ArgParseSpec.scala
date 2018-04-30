package org.rwtodd.argparse

import org.scalatest._

class ArgParseSpec extends Spec {

  object `An Args Instance` {
     def `should run a does func` = {
       var g = 0
       val as = new Args(new FlagArg("-g","increment g").does { _ => g += 1 })
       val extra = as.parse(Array("-g","-g"))
       assert(extra.isEmpty)
       assert(g == 2) 
       as.parse(Array("-g","-g"))
       assert(g == 4) 
     }

     def `should set a double` = {
       var g = 0.0
       val as = new Args(new DoubleArg("-dbl","set g").does { g = _ })
       val extra = as.parse(Array("-dbl","2.5"))
       assert(extra.isEmpty)
       assert(g == 2.5) 
     }

     def `should take the last seen value` = {
       var g = 0.0
       val as = new Args(new DoubleArg("-dbl","set g").does { g = _ })
       val extra = as.parse(Array("-dbl","2.5","-dbl","4.5"))
       assert(extra.isEmpty)
       assert(g == 4.5)
     }

     def `should collect extra params` = {
       var g = 0.0
       val as = new Args(new DoubleArg("-dbl","set g").does { g = _ })
       val extra = as.parse(Array("-dbl","2.5","a","-dbl","4.5","b"))
       assert(extra.length == 2)
       assert(extra(0) == "a")
       assert(extra(1) == "b")
     }

     def `should throw on missing args` = {
       var g = 0.0
       val as = new Args(new DoubleArg("-dbl","set g").does { g = _ })
       intercept[IllegalArgumentException] { 
          as.parse(Array("-dbl"))
       }
     }

     def `should throw on non-number` = {
       var g = 0.0
       var i = 0
       val as = new Args(new DoubleArg("-g","set g").does { g = _ },
                         new IntArg("-i", "set i").does { i = _ })
       intercept[IllegalArgumentException] { 
          as.parse(Array("-g","2.5a"))
       }
       intercept[IllegalArgumentException] { 
          as.parse(Array("-i","2a"))
       }
     }
  
     def `should allow restricted choices` = {
       var g = 0
       val as = new Args(new IntArg("-opt","set g").choices(5 to 10).does { g = _ })
       val extra = as.parse(Array("-opt","5"))
       assert(extra.isEmpty)
       assert(g == 5)
       intercept[IllegalArgumentException] { 
          as.parse(Array("-opt","12"))
       }
     }

     def `should allow defaults` = {
       var g = ""
       val as = new Args(new StrArg("-name", "gives a name").defaultsTo("Orange").does { g = _ })
       val extra = as.parse(Array("hi"))
       assert(extra.length == 1)
       assert(g == "Orange") 
     }

     def `should ignore default when explicity set` = {
       var g = ""
       val as = new Args(new StrArg("-name", "gives a name").defaultsTo("Orange").does { g = _ })
       val extra = as.parse(Array("-name", "scala"))
       assert(extra.isEmpty)
       assert(g == "scala") 
     }

  }

}
