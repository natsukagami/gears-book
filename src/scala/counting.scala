//> using scala 3.4.0
//> using dep "ch.epfl.lamp::gears::0.2.0-SNAPSHOT"
//> using nativeVersion "0.5.0-RC1"

import gears.async.*
import gears.async.default.given

/** Counts to [[n]], sleeping for 100milliseconds in between. */
def countTo(n: Int)(using Async): Unit =
  (1 to n).foreach: i =>
    AsyncOperations.sleep(100 /* milliseconds */ ) /*(using Async)*/
    println(s"counted $i")

@main def Counting() =
  Async.blocking: /* (async: Async) ?=> */
    countTo(10)
    println("Finished counting!")
