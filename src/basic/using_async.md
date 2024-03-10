# An "Async" function

The central concept of Gears programs are functions taking an [`Async`](https://lampepfl.github.io/gears/api/gears/async/Async.html) context,
commonly referred to in this book as an _async function_.

```scala
import gears.async.*

def asyncFn(n: Int)(using Async): Int =
                       // ^^^^^ an async context
    AsyncOperations.sleep(n * 100 /* milliseconds */)/*(using Async)*/
    n + 1
```

We can look at the `Async` context from two points of view, both as a **capability**
and a **context**:
- As a Capability, `Async` represents the ability to *suspend* the current computation,
  waiting for some event to arrive (such as timeouts, external I/Os or even other concurrent computations).
  While the current computation is suspended, we allow the runtime to use the CPU for
  other tasks, effectively utilizing it better
  (compared to just blocking the computation until the wanted event arrives).
- As a Context, `Async` holds the neccessary runtime information for the scheduler
  to know *which computation* should be suspended and how to *resume* them later.
  It also contains a *concurrency scope*, which we will see later in the [structured concurrency section](./structured_concurrency.md).

However, different from other languages with a concept of Async functions, `gears`'s async functions are
just normal functions with an implicit `Async` parameter!
This means we can also explicitly take the parameter as opposed to `using Async`:
```scala
def alsoAsyncFn(async: Async): Int =
    asyncFn(10)(using async)
```
and do *anything* with the Async context as if it was a variable[^cap_rules]!

## Passing on `Async` contexts

Let's look at a more fully-fledged example, `src/scala/counting.scala`:

```scala 3
{{#include ../scala/counting.scala}}
```

(if you see `//>` directive on examples, it means the example can be run self-contained!)

Let's look at a few interesting points in this example:
1. [`Async.blocking`](https://lampepfl.github.io/gears/api/gears/async/Async$.html#blocking-9c3):
   `Async.blocking` gives you a "root" `Async` context, given implementations of the supporting layer
   (neatly provided by `import gears.async.default.given`!).

   This defines a "scope", not too different from [`scala.util.Using`](https://www.scala-lang.org/api/current/scala/util/Using$.html)
   or [`scala.util.boundary`](https://www.scala-lang.org/api/current/scala/util/boundary$.html). As usual, the context provided
   within the scope should not escape it.
   Like `scala.util.boundary`'s `boundary.Label`, the `Async` context will be passed implicitly to other functions taking `Async` context.
   Note that `Async.blocking` is the **only** provided mean of creating an `Async` context out of "nothing", so it is easy to keep track of
   when [adopting `gears` into your codebase](../usage/adopting-gears.md)!
2. `countTo` returns `Unit` instead of `Future[Unit]` or an equivalent. This indicates that *calling `countTo` will "block" the caller
   until `countTo` has returned with a value. No `await` needed, and `countTo` behaves just like a normal function!

   Of course, with the `Async` context passed, `countTo` is allowed to perform operations that suspends, such as
   [`sleep`](https://lampepfl.github.io/gears/api/gears/async/AsyncOperations$.html) (which suspends the `Async` context for the
   given amount of milliseconds).

   This illustrates an important concept in Gears: *in most common cases, we write functions that accepts a suspendable context and
   calling them will block until they return*!
   While it is completely normal to spawn concurrent/parallel computations and join/race them, as we will see in the next chapter,
   "natural" APIs written with Gears should have the same control flow as non-`Async` functions.
3. Within `countTo`, note that we call `sleep` *under a function passed into `Seq.foreach`*, effectively *capturing* `Async` within the
   function.
   This is completely fine: `foreach` runs the given function in the same `Async` context (not outside nor in a sub-context), and does
   not capture the function. Our capability and scoping rules is maintained, and `foreach` is `Async`-capable by default!

   While this illustrates the above fact, we could've just written the function in a familiar fashion with `for` comprehension:
   ```scala 3
   /** Counts to [[n]], sleeping for 100milliseconds in between. */
   def countTo(n: Int)(using Async): Unit =
     for i <- 1 to n do
       AsyncOperations.sleep(100.millis)
       println(s"counted $i")
   ```

That's all for now with the `Async` context. Next chapter, we will properly introduce concurrency to our model.

## Aside: Pervasive `Async` problem?

At this point, if you've done asynchronous programming with JavaScript, Rust or C# before, you might wonder if
Gears is offering a solution that comes with
the [What color is your function?](https://journal.stuffwithstuff.com/2015/02/01/what-color-is-your-function/) problem.

It is true that the `Async` context divides the function space into ones requiring it and ones that don't,
and generally you need an `Async` context to call an async function[^async_blocking_possible].
However:
- Writing async-polymorphic higher order functions is trivial in Gears: should you not be caring about `Async` contexts
  when taking in function arguments (`() => T` and `Async ?=> T` both works), simply take `() => T` and `Async`-aware
  blocks will inherit the context from the caller!

  One obvious example is `Seq.foreach` from above. In fact, **all** current Scala collection API should still work with no changes.
  Of course, if applied in repeat the function will be run sequentially rather than concurrently, but that *is* the
  expected behavior of `Seq.foreach`.
- The precense of an `Async` context helps explicitly declaring that a certain function requires runtime support for
  suspension, as well as the precense of a concurrent scope (for cancellation purposes).
  Ultimately, that means the compiler does not have to be pessimistic about compiling all functions in a suspendable
  way (a la Go), hurting both performance and interopability (especially with C on Scala Native).

  For the user, is also a clear indication that calling the function will suspend to wait for (in most cases) external
  events (IO, filesystem, sleep, ...), and should prepare accordingly.
  Likewise, libraries using Gears should *not* be performing such tasks if they don't take an `Async` context.

With that in mind, it is useful (as with all languages with async functions) to treat `Async` like a capability,
only pass them in functions handling async operations, and try to isolate business logic into functions that don't.

[^cap_rules]: While in principle this is possible, capability and scoping rules apply to the Async context: functions taking
`Async` capabilities should not capture it in a way that stays longer than the function's body execution.
In the future, capture checking should be able to find such violations and report them during the compilation process.
[^async_blocking_possible]: Technically `Async.blocking` can be used to call any async function without an async context,
you should be aware of [its pitfalls](../idiomatic/blocking.md). That said, if you are migrating from a synchronous codebase,
they are functional bridges between migrated and in-progress parts of the codebase.
