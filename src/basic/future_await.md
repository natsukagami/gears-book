# Working with multiple Futures

While writing code that performs computation concurrently,
it is common to spawn multiple futures and then await for them at the same time,
with some particular logic.

Gears provides some tools to deal with multiple futures, both for a statically known number and
unknown number of futures (i.e. if you have a `Seq[Future[T]]`).

## Combining two futures

[`Future.zip`](https://lampepfl.github.io/gears/api/gears/async/Future$.html#zip-fffff47f)
takes two futures `Future[A]` and `Future[B]` and returns a `Future[(A, B)]`,
completing when both input futures do.
```scala
Async.blocking:
  val a = Future(1)
  val b = Future("one")
  val (va, vb) = a.zip(b).await // (1, "one")
```
if one of the input futures throw an exception, it gets re-thrown as soon as possible (i.e. without
suspending until the other completes).

[`Future.or`](https://lampepfl.github.io/gears/api/gears/async/Future$.html#or-bae)
takes two `Future[T]`s and returns another `Future[T]`, completing with the *first value*
that one of the two futures *succeeds* with.
If both throws, throw the exception that was thrown *last*.
```scala
Async.blocking:
  val a = Future(1)
  val b = Future:
    AsyncOperations.sleep(1.hour)
    2
  val c = Future:
    throw Exception("explode!")

  a.or(b).await // returns 1 immediately
  b.or(a).await // returns 1 immediately
  c.or(a).await // returns 1 immediately
  c.or(c).await // throws

  a.orWithCancel(b).await // returns 1 immediately, and b is cancelled
```
[`Future.orWithCancel`](https://lampepfl.github.io/gears/api/gears/async/Future$.html#orWithCancel-bae)
is a variant of `Future.or` where the slower `Future` is cancelled.

## Combining a Sequence of futures

[`Seq[Future[T]].awaitAll`](https://lampepfl.github.io/gears/api/gears/async/Future$.html#awaitAll-fffffe4f)
takes a `Seq[Future[T]]` and return a `Seq[T]` when *all* futures in the input `Seq`
are completed.
The results are in the same order corresponding to the `Future`s of the original `Seq`.
If one of the input futures throw an exception, it gets re-thrown as soon as possible (i.e. without
suspending until the other completes).

It is a more performant version of `futures.fold(_.zip _).await`.
```scala
Async.blocking:
    val a = Future(1)
    val b = Future:
      AsyncOperations.sleep(1.second)
      2
    val c = Future:
      throw Exception("explode!")

    Seq(a, b).awaitAll // Seq(1, 2), suspends for a second
    Seq(a, b, c).awaitAll // throws immediately
```
[`awaitAllOrCancel`](https://lampepfl.github.io/gears/api/gears/async/Future$.html#awaitAllOrCancel-fffffe4f)
is a variant where all futures are cancelled when one of them throws.

[`Seq[Future[T]].awaitFirst`](https://lampepfl.github.io/gears/api/gears/async/Future$.html#awaitFirst-fffffcb7)
takes a `Seq[Future[T]]` and return a `T` when *the first* future in the input `Seq` *succeeds*.
If all input futures throw an exception, the last exception is re-thrown.

It is a more performant version of `futures.fold(_.or _).await`.
```scala
Async.blocking:
    val a = Future(1)
    val b = Future:
      AsyncOperations.sleep(1.second)
      2
    val c = Future:
      throw Exception("explode!")

    Seq(a, b).awaitFirst    // returns 1 immediately
    Seq(a, b, c).awaitFirst // returns 1 immediately
```
[`awaitFirstWithCancel`](https://lampepfl.github.io/gears/api/gears/async/Future$.html#awaitFirstWithCancel-fffffcb7)
is a variant where all other futures are cancelled when one succeeds.

## `Async.select`

[`Async.select`](https://lampepfl.github.io/gears/api/gears/async/Async$.html#)
works similar to `awaitFirst`, but allows you to attach a body to handle the returned value, so the futures
being raced does not have to be of the same type (as long as the handler returns the same type).

```scala
Async.blocking:
  val a = Future(1)
  val b = Future("one")

  val v = Async.select(
    a.handle: va =>
      s"number $va was returned",
    b.handle: vb =>
      s"string `$vb` was returned"
  )

  println(v) // either "number 1 was returned", or "string `one` was returned"
```

`Async.select` takes `SelectCase*` varargs, so you can also `.map` a sequence of `Future` as well. No need to create more futures!
```scala
Async.blocking:
  val futs = (1 to 10).map(Future.apply)
  val number = Async.select(futs.map(_.handle: v => s"$v returned")*)
```

## `Future.Collector`

[`Future.Collector`](https://lampepfl.github.io/gears/api/gears/async/Future$$Collector.html)
takes a sequence of `Future` and [exposes](https://lampepfl.github.io/gears/api/gears/async/Future$$Collector.html#results-0)
a [`ReadableChannel`](https://lampepfl.github.io/gears/api/gears/async/ReadableChannel.html)[^channel]
returning the Futures as they are completed.

`Collector` allows you to manually implement handling of multiple futures as they arrive, should you need
more complex behaviors than the ones provided.

For example, here is a simplified version of `.awaitFirst`:
```scala
def awaitFirst[T](futs: Seq[Future[T]])(using Async): T =
  val len = futs.length
  val collector = Future.Collector(futs*)
  @tailrec def loop(failed: Int): T =
    val fut = collector.results.read()
    fut.awaitResult match
      case Success(value) => value
      case Failure(exc) =>
        if failed + 1 == len then throw exc
        else loop(failed + 1)
  loop(0)
```

[`MutableCollector`](https://lampepfl.github.io/gears/api/gears/async/Future$$MutableCollector.html) is a `Collector`
that also lets you [`add`](https://lampepfl.github.io/gears/api/gears/async/Future$$MutableCollector.html#add-10b) more
`Future`s after creation.

[^channel]: learn more about channels in a [future chapter](./channels.md).
