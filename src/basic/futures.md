# Concurrency with Future

[`Future`](https://lampepfl.github.io/gears/api/gears/async/Future.html)
is the primary source of concurrency in a Gears program.
There are actually two kinds of futures: passive and active. However,
we shall look at active futures for now, and come back with passive futures
in [the next chapter](../unstructured/sources.md).

## Spawning a concurrent computation with `Future.apply`

[`Future.apply`](https://lampepfl.github.io/gears/api/gears/async/Future$.html#apply-fffffebf)
takes a `body` of the type `Async.Spawn ?=> T` (that is, an async computation)
and runs it *completely concurrently* to the current computation.
`Future.apply` returns immediately with a value of type `Future[T]`.
Eliding details (that we shall come back next chapter), there are two things you can do with this `Future[T]`:
- **Await**: calling [`.await`](https://lampepfl.github.io/gears/api/gears/async/Async$.html#await-5df)
  on the `Future[T]` _suspends_ the current Async context until the `Future`'s body
  returns, and give you back the returned value `T`.
  If `body` throws an exception, `.await` will also throw as well. `.awaitResult` is the alternative where both
  cases are wrapped inside a `Try`.

  Being a _suspension point_, `.await` requires an Async context.
- **Cancel**: calling [`.cancel()`](https://lampepfl.github.io/gears/api/gears/async/Future.html#cancel-94c)
  on the `Future[T]` *sends* a cancellation signal to the `Future`.
  This would cause `body`'s execution to receive `CancellationException` on the next suspension point,
  and cancel all of the `body`'s spawned Futures and so on, in a cascading fashion.

  After cancelling, `.await`-ing the `Future` will raise a `CancellationException`.

### An example

We can have a look at one simple example, implementing my favorite sorting algorithm, sleepsort!

```scala 3
{{#include ../scala/sleepsort.scala}}
```

Let's walk through what's happening here:
1. Starting from a `Seq[Int]`, we `.map` the elements each to create a `Future[Unit]`. Calling `Future:`
   gives us a new `Async` context, passed into the `Future`'s body.
   This `Async` context inherits the suspension implementation from `Async.blocking`'s context, and has a
   sub-scope with `Async.blocking`'s context as its parent. We will talk about scoping in the next section.
2. In each `Future`, we `sleep` for the amount of milliseconds the same as the element's value itself.
   Note that `sleep` would suspend the `Async` context given by the `Future`, i.e. the future's body, but *not*
   the one running under `Async.blocking`.
   Hence, it is totally okay to have thousands of `Future`s that `sleep`!
3. `.map` gives us back a `Seq[Future[Unit]]` immediately, which we can wait for _all_ futures to complete
   with the extension method [`.awaitAll`].
   This suspends `Async.blocking` context until all futures run to completion, and gives us back `Seq[Unit]`.
   The return value is not interesting though, what we care about is the `buf` at the end.

## `Async.Spawn`

If you noticed, `Async.blocking` gives you an [`Async.Spawn`](https://lampepfl.github.io/gears/api/gears/async/Async$.html#Spawn-0) context
(rather than just `Async`), which `Future.apply`
[requires](https://lampepfl.github.io/gears/api/gears/async/Future$.html#apply-fffffebf). What is it?

Recall that `Async`, as a capability, gives you the ability to _suspend_ the computation to wait for other values.
`Async.Spawn` adds a new capability on top: it allows you to _spawn_ concurrent computations as well.

Getting a `Spawn` capability is actually very simple. You get an `Async.Spawn` capability on `Async.blocking` by default,
and both `Future.apply` and `Async.group` gives you an `Async.Spawn` capability sub-scoped from a parent `Async` scope.
Note that, however, most functions do _not_ take an `Async.Spawn` context by default.
This is due to the nature of `Spawn`'s capability to spawn computations that _runs as long as the `Spawn` scope is alive_, which typically corresponds to the lexical scope of the function.
If functions take `Async.Spawn`, they are allowed to spawn futures that are still computing even _after_ the function itself returns!
```scala
def fn()(using Async.Spawn) =
    val f = Future:
        useCPUResources()
    0

Async.blocking:
    val n = fn()
    // f is still running at this point!!!
```
This is way more difficult to reason about, so the "sane" behavior is to _not_ take `Async.Spawn` by default, and only
"upgrading" it (with `Async.group`) inside a scope that will take care of cleaning up these "dangling" futures on return.

Most of Gear's library are structured this way, and we strongly recommend you to do the same in your function designs.
