//> using scala 3.4.0
//> using dep "ch.epfl.lamp::gears::0.2.0"
//> using nativeVersion "0.5.0-RC1"

import scala.collection.mutable
import scala.concurrent.duration.*
import gears.async.*
import gears.async.default.given

@main def sleepSort() =
  Async.blocking: /* (spawn: Async.Spawn) ?=> */
    val origin = Seq(50, 80, 10, 60, 40, 100)
    // Spawn sleeping futures!
    val buf = mutable.ArrayBuffer[Int]()
    origin
      .map: n =>
        Future /*(using spawn)*/: /* (Async) ?=> */
          AsyncOperations.sleep(n.millis)
          buf.synchronized:
            buf += n
      .awaitAll
    println(buf)
