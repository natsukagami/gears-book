//> using scala 3.4.0
//> using dep "ch.epfl.lamp::gears::0.2.0"
//> using nativeVersion "0.5.0-RC1"

import gears.async.*
import gears.async.default.given

@main def main() =
  Async.blocking:
    val hello = Future:
      print("Hello")
    val world = Future:
      hello.await
      println(", world!")
    world.await
