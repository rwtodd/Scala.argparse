package org.rwtodd.argparse

import scala.collection.mutable.ArrayBuffer

abstract class Switch(val name: String, val help: String, val needsArg: Boolean) {
  def accept(a: Args, v: String) : Unit
  def applyDefault() = {}
}

/** A base class for cmdline switches. */
abstract class TypedSwitch[T](name: String, help: String) extends Switch(name, help, true) {
  protected var seen = false
  protected var action: (T) => Unit = null
  protected var options: Seq[T] = null
  protected var default: Option[T] = None

  def defaultsTo(v: T) = {
     default = Some(v)
     this
  }

  def does(what: (T) => Unit) = {
     action = what
     this
  }

  def choices(opts : Seq[T]) = {
     options = opts
     this
  }

  def parse(v: String): T

  override def accept(a: Args, v: String) : Unit = {
     seen = true
     val current = parse(v)
     if((options == null) || (options.contains(current))) {
       action(current)
     } else {
       throw new IllegalArgumentException(s"<${v}> isn't a valid argument to switch <${name}>!")
     }
  }

  override def applyDefault() : Unit =
    if(!seen) {
      default match {
         case Some(t) => action(t)
         case None    => /* Nothing */
      }
    }
}

/** A class for switches that take integers */
class IntArg(name: String, help: String) extends TypedSwitch[Int](name,help) {
  override def parse(v: String) : Int = try {
     v.toInt
  } catch {
     case e: Exception =>
        throw new IllegalArgumentException(s"Switch <${name}> expected an integer instead of <${v}>.", e)
  }
}

/** A class for switches that take doubles */
class DoubleArg(name: String, help: String) extends TypedSwitch[Double](name,help) {
  override def parse(v: String) : Double = try {
     v.toDouble
  } catch {
     case e: Exception =>
        throw new IllegalArgumentException(s"Switch <${name}> expected a number instead of <${v}>.", e)
  }
}

/** A class for switches that take strings */
class StrArg(name: String, help: String) extends TypedSwitch[String](name,help) {
  override def parse(v: String) : String = v
}

/** A class for switches that are no-arg flags */
class FlagArg(name: String, help: String) extends Switch(name,help,false) {
  protected var action : () => Unit = null

  override def accept(a: Args, v: String) : Unit = {
    if (action != null) action()
  }

  def does(what: () => Unit) = {
     action = what
     this
  }
}

class HelpArg(name: String) extends Switch(name, "displays this help text", false) {
  protected var preText: String = ""
  protected var postText: String = ""

  def saysFirst(sf: String) = {
    preText = sf
    this
  }

  def saysLast(sl: String) = {
    postText = sl
    this
  }

  override def accept(a: Args, v: String): Unit = {
    System.err.println(preText)
    a.showOptions(System.err)
    System.err.println(postText)
  }
}


/** Args is the class that actually parses a set of arguments */
class Args(val switches: Switch*) {
  def parse(args: IndexedSeq[String]) : ArrayBuffer[String] = {
      val extras = ArrayBuffer[String]()
      var idx = 0
      val alen = args.length
      while (idx < alen) {
          val hd = args(idx)
          idx += 1
          switches.find(sw => sw.name == hd) match {
            case Some(sw) => if(sw.needsArg) {
                                 if (idx == alen) {
                                     throw new IllegalArgumentException(
                                        s"The switch <${sw.name}> expects an argument!")
                                 }
                                 sw.accept(this, args(idx))
                                 idx += 1
                             } else {
                                 sw.accept(this, "")
                             }
            case None     => extras += hd
          }
      }
      for (sw <- switches) sw.applyDefault()
      extras
  }

  /** generates a helpful listing of the available arguments.
   *  Users should subclass this to elaborate on what it says, calling
   *  the super function when it's time to show the arguments. */
  def showOptions(wtr: java.io.PrintStream) : Unit = {
     val helpStrings = switches map { sw =>
        val h = sw.help
        if (h.startsWith("<")) {
          val (argname, helpstr) = h.splitAt(h.indexOf('>')+1)
          ( s"${sw.name} ${argname}", helpstr.trim )
        } else (sw.name, h)
     }
     wtr.println("OPTIONS")
     for { (nm ,desc) <- helpStrings } {
        if (nm.length <= 5)  wtr.println(f"  $nm%-5s  $desc%s")
        else {
           wtr.println(s"  $nm")
           wtr.println(s"         $desc")
        }
        wtr.println()
     }
  }
}

