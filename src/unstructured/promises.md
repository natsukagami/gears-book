# Passive Futures and Promises

As mentioned in the last section, the `Future` trait represents a persistent source that is also cancellable.
```scala
trait Future[+T] extends Source[Try[T]], Cancellable
```

As mentioned before, `Future` can be active or passive[^passive-reactive].
We have delved deeply into active `Future`s, and hence this section is all about passive `Future`s.

[^passive-reactive]: Even passive `Future`s are considered an active `Source` by our definition.

So what are passive `Future`s? Gears make a distinction between an active and a passive `Future` based on whether
its computation is within or outside Gears' structured concurrency.
Active `Future`s are actively driven by Gears, and passive `Future`s can be completed from outside of Gears' machineries.

Typically, passive `Future`s are used as a straightforward way to encapsulate callback-style functions into
Gears interface.
We've seen how the interface is created in the last section, we shall now see how we can provide the implementation.

## `Future.withResolver`: JS-style callback-to-future conversion

Here is an example on how to implement a `Future`-returning version of `readString` given the callback one:
```scala
object File:
  def readString(path: String, callback: Either[Error, String] => Unit): Cancellable =
    // provided
    ???

  def readString(path: String): Future[Either[Error, String]] =
    Future.withResolver: resolver =>
      val cancellable = readString(path, result => resolver.resolve(result))
      resolver.onCancel: () =>
        cancellable.cancel()
        resolver.rejectAsCancelled()
```

- [`Future.withResolver`](https://lampepfl.github.io/gears/api/gears/async/Future$.html#withResolver-fffff6bf) creates a
  passive `Future` where completion is managed by the
  [`Resolver`](https://lampepfl.github.io/gears/api/gears/async/Future$$Resolver.html) parameter passed into `body`.

  The role of `body` is to set up callbacks that will complete the `Future` by
  [resolving it with a value](https://lampepfl.github.io/gears/api/gears/async/Future$$Resolver.html#resolve-fffff71f) or
  [failing it with an exception](https://lampepfl.github.io/gears/api/gears/async/Future$$Resolver.html#reject-4b8).

  In the case that the callback function is cancellable, `body` can set up through
  [`Resolver.onCancel`](https://lampepfl.github.io/gears/api/gears/async/Future$$Resolver.html#onCancel-9dc)
  to forward the cancellation of the `Future` into the callback function.
- Unlike `Future.apply`, `withResolver` runs body directly, blocking the caller until `body` is complete. It runs the
  `Future` *after* `body` has completed setting it up.

If you are familiar with JavaScript, this is almost the same interface as its
[`Promise` constructor](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/Promise).

## Converting between Scala and Gears futures

Since Scala [`scala.concurrent.Future`](https://www.scala-lang.org/api/current/scala/concurrent/Future.html#) runs with their
own `ExecutionContext`, it is considered outside of Gears machinary, and hence can only be converted into a passive Gears `Future`.

This can be done by importing
[`ScalaConverters`](https://lampepfl.github.io/gears/api/gears/async/ScalaConverters$.html)
and using the extension method `.asGears` on the `Future`.

Note that it requires an `ExecutionContext`, in order to run the code that would `complete` the `Future`.
Cancelling this `Future` only forces it to return `Failure(CancellationException)`, as Scala Futures cannot be cancelled externally.

Converting from a Gears `Future`, passive or active, to a Scala `Future` is also possible through the
[`.asScala`](https://lampepfl.github.io/gears/api/gears/async/ScalaConverters$.html#asScala-fffff4e2)
extension method.
Futures created from active Gears futures will return `Failure(CancellationException)` once they go out of scope.

## Manual completion of Futures with `Promise`

In case you need a more complicated flow than a callback, Gears provide a more manual way of creating passive `Future`s,
through a [`Future.Promise`](https://lampepfl.github.io/gears/api/gears/async/Future$$Promise.html).

Simply put, it is a `Future` of which you can manually complete with a value
[through a method](https://lampepfl.github.io/gears/api/gears/async/Future$$Promise.html#).

Because there is no structure to `Promise`, it is never recommended to return one directly. It should be upcasted
to `Future` (explicitly or with [`.asFuture`](https://lampepfl.github.io/gears/api/gears/async/Future$$Promise.html#asFuture-0))
before returned.

One cannot forward cancellation of a `Promise`.
