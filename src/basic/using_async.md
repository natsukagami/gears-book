# An "Async" function

The central concept of Gears programs are functions taking an `Async` context,
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
1. `Async.blocking`: `Async.blocking` gives you a "root" `Async` context, given implementations of the supporting layer
   (neatly provided by `import gears.async.default.given`!).

[^cap_rules]: While in principle this is possible, capability and scoping rules apply to the Async context: functions taking
`Async` capabilities should not capture it in a way that stays longer than the function's body execution.
In the future, capture checking should be able to find such violations and report them during the compilation process.
