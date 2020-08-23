package org.rwtodd.argparse

import org.scalatest.flatspec.AnyFlatSpec

class ArgParseSpec extends AnyFlatSpec {
	behavior of "An Args Instance"
	it should "run a does func" in {
		var g = 0
		val as = new Args(new FlagArg("-g","increment g").does { () => g += 1 })
		val extra = as.parse(Array("-g","-g"))
		assert(extra.isEmpty)
		assert(g == 2) 
		as.parse(Array("-g","-g"))
		assert(g == 4) 
	}

	it should "set a double" in {
		var g = 0.0
		val as = new Args(new DoubleArg("-dbl","set g").does { g = _ })
		val extra = as.parse(Array("-dbl","2.5"))
		assert(extra.isEmpty)
		assert(g == 2.5) 
	}

	it should "take the last seen value" in {
		var g = 0.0
		val as = new Args(new DoubleArg("-dbl","set g").does { g = _ })
		val extra = as.parse(Array("-dbl","2.5","-dbl","4.5"))
		assert(extra.isEmpty)
		assert(g == 4.5)
	}

	it should "collect extra params" in {
		var g = 0.0
		val as = new Args(new DoubleArg("-dbl","set g").does { g = _ })
		val extra = as.parse(Array("-dbl","2.5","a","-dbl","4.5","b"))
		assert(extra.length == 2)
		assert(extra(0) == "a")
		assert(extra(1) == "b")
	}

	it should "throw on missing args" in {
		var g = 0.0
		val as = new Args(new DoubleArg("-dbl","set g").does { g = _ })
		assertThrows[IllegalArgumentException] { 
			as.parse(Array("-dbl"))
		}
	}

	it should "throw on non-number" in {
		var g = 0.0
		var i = 0
		val as = new Args(new DoubleArg("-g","set g").does { g = _ },
		new IntArg("-i", "set i").does { i = _ })
		assertThrows[IllegalArgumentException] { 
			as.parse(Array("-g","2.5a"))
		}
		assertThrows[IllegalArgumentException] { 
			as.parse(Array("-i","2a"))
		}
	}

	it should "allow restricted choices" in {
		var g = 0
		val as = new Args(new IntArg("-opt","set g").choices(5 to 10).does { g = _ })
		val extra = as.parse(Array("-opt","5"))
		assert(extra.isEmpty)
		assert(g == 5)
		assertThrows[IllegalArgumentException] { 
			as.parse(Array("-opt","12"))
		}
	}

	it should "allow defaults" in {
		var g = ""
		val as = new Args(new StrArg("-name", "gives a name").defaultsTo("Orange").does { g = _ })
		val extra = as.parse(Array("hi"))
		assert(extra.length == 1)
		assert(g == "Orange") 
	}

	it should "ignore default when explicity set" in {
		var g = ""
		val as = new Args(new StrArg("-name", "gives a name").defaultsTo("Orange").does { g = _ })
		val extra = as.parse(Array("-name", "scala"))
		assert(extra.isEmpty)
		assert(g == "scala") 
	}

}

// vim: filetype=scala:noet:tabstop=4:softtabstop=0:shiftwidth=0:
