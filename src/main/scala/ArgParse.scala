package org.rwtodd.argparse

import scala.collection.mutable.ArrayBuffer

/** A base class for cmdline switches. */
abstract class Switch[T](val name: String, val help: String) {
  val needsArg = true 

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

  def accept(v: String) : Unit = {
     seen = true
     val current = parse(v)
     if((options == null) || (options.contains(current))) {
       action(current)
     } else {
       throw new IllegalArgumentException(s"<${v}> isn't a valid argument to switch <${name}>!")
     }
  }

  def applyDefault() : Unit = 
    if(!seen) {
      default match {
         case Some(t) => action(t)
         case None    => /* Nothing */
      }
    }
}

/** A class for switches that take integers */ 
class IntArg(name: String, help: String) extends Switch[Int](name,help) {
  override def parse(v: String) : Int = try { 
     v.toInt
  } catch {
     case e: Exception => 
        throw new IllegalArgumentException(s"Switch <${name}> expected an integer instead of <${v}>.", e)
  }
}

/** A class for switches that take doubles */ 
class DoubleArg(name: String, help: String) extends Switch[Double](name,help) {
  override def parse(v: String) : Double = try {
     v.toDouble
  } catch {
     case e: Exception => 
        throw new IllegalArgumentException(s"Switch <${name}> expected a number instead of <${v}>.", e)
  }
}

/** A class for switches that take strings */ 
class StrArg(name: String, help: String) extends Switch[String](name,help) {
  override def parse(v: String) : String = v 
}

/** A class for switches that are no-arg flags */ 
class FlagArg(name: String, help: String) extends Switch[Boolean](name,help) {
  override val needsArg = false 
  override def parse(v: String) : Boolean = true
}


/** Args is the class that actually parses a set of arguments */
class Args(val switches: Switch[_]*) {
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
                                 sw.accept(args(idx)) 
                                 idx += 1
                             } else { 
                                 sw.accept("") 
                             }
            case None     => extras += hd
          }
      }
      for (sw <- switches) sw.applyDefault() 
      extras 
  }  

  /** Tuples of (name, desc) with the argument-name from the front of the 
   *  help string pulled into the name portion, and the rest put into the
   *  desc portion.  */
  private lazy val helpStrings = switches map { sw =>
     val h = sw.help
     if (h.startsWith("<")) {
       val (argname, helpstr) = h.splitAt(h.indexOf('>')+1)
       ( s"${sw.name} ${argname}", helpstr.trim ) 
     } else (sw.name, h)
  }

  /** generates a helpful listing of the available arguments.
   *  Users should subclass this to elaborate on what it says, calling
   *  the super function when it's time to show the arguments. */
  def showHelp() : Unit = {
     println("OPTIONS")
     for { (nm ,desc) <- helpStrings } {
        if (nm.length <= 5)  println(f"  $nm%-5s  $desc%s")
        else {
           println(s"  $nm")
           println(s"         $desc")
        }
        println() 
     }
  }
}

