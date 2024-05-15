//> using scala 3.4.0
//> using dep "ch.epfl.lamp::gears::0.2.0"
//> using nativeVersion "0.5.0-RC1"
//> using nativeMode "release-full"

import java.io.Closeable
import scala.annotation.tailrec
import scala.util.boundary
import gears.async.*
import gears.async.default.given

// Sender that owns the channel
type OwnedSender[T] = SendableChannel[T] & Closeable

// Send the sequence 2, 3, 4, ..., until and then close the channel
def generate(until: Int)(ch: OwnedSender[Int])(using Async) =
  for i <- 2 to until do ch.send(i)
  ch.close()

// Filter out multiples of k
def sieve(
    k: Int
)(in: ReadableChannel[Int], out: OwnedSender[Int])(using Async) =
  @tailrec def loop(): Unit =
    in.read() match
      case Left(_) => ()
      case Right(n) =>
        if n % k != 0 then out.send(n)
        loop()
  loop()
  out.close()

@main def PrimeSieve(n: Int) =
  Async.blocking:
    // set up sieves
    val inputChan = SyncChannel[Int]()
    // start generating numbers
    Future(generate(n)(inputChan))

    // Collect answers
    @tailrec def loop(input: ReadableChannel[Int]): Unit =
      input.read() match
        // no more numbers
        case Left(_) => ()
        // new prime number
        case Right(n) =>
          println(s"$n is prime")
          // filter out multiples of n
          val chan = SyncChannel[Int]()
          Future(sieve(n)(input, chan))

          loop(chan)

    loop(inputChan)
