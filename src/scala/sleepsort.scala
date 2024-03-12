//> using scala 3.4.0
//> using dep "ch.epfl.lamp::gears::0.2.0-SNAPSHOT"
//> using nativeVersion "0.5.0-RC1"

import scala.collection.mutable
import gears.async.*
import gears.async.default.given

@main def sleepSort() =
  Async.blocking:
    val origin = Seq(50, 80, 10, 60, 40, 100)
    // Spawn sleeping futures!
    val buf = mutable.ArrayBuffer[Int]()
    origin
      .map: n =>
        Future:
          AsyncOperations.sleep(n)
          buf.synchronized:
            buf += n
      .awaitAll
    println(buf)
