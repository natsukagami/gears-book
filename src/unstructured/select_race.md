# Select and Race

`Async.select` works on both Channels and Futures because they operate on `Source`s.
Its behavior generalizes to taking all `Source`s, with **exactly one** item consumed from one of the sources,
of which the corresponding handler is run.

## `Async.select` machinery

This behavior is achieved by passing into the `Source`s a special `Listener` that would only accept the
first item resolved, and rejecting all others.

Learn more about failible in [the next section](./listeners.md).

## `Async.race`

Under the hood, `Async.select` uses a more general method to combine multiple `Source`s into one
with a racing behavior.

```scala
object Async:
  def race[T](sources: Source[T]*): Source[T]
```

`race` takes multiple sources and returns a new ephemeral, reactive `Source` that:
- once a listener is attached, it wraps the listener with a one-time check and forwards it to all sources.
- when one source resolves this listener with a value, invalidates this listener on all other sources.

Note that `Source.dropListener` is inherently racy: one cannot reliably use `dropListener` to *guarantee*
that a listener will *not* be run.
This is the main motivation to introduce failible listeners.

[`Async.raceWithOrigin`](https://lampepfl.github.io/gears/api/gears/async/Async$.html#raceWithOrigin-fffff069) is
a variant of `race` that also returns *which* of the origin sources the item comes from.

## Calling listeners and `Source.transformValuesWith`

From the description of `race`, you might notice a general pattern for transforming `Source`s with a `.map`-like
behavior:
- Given a transformation function `f: T => U`, one can transform a `Source[T]` to a `Source[U]` by creating a Source that...
- Given a listener `l: Listener[U]`, create a `Listener[T]` that applies `f` before passing to `l`...
- ... and pass that wrapping listener back to `Source[T]`.

And this is exactly what
[`Source.transformValuesWith`](https://lampepfl.github.io/gears/api/gears/async/Async$.html#transformValuesWith-551) does.

This looks like a very useful method, so why isn't it a basic concept, with a familiar name (`.map`)?
The devil lies in the details, specifically how `Source` notifies its `Listener`s.

Notice that there is no `ExecutionContext` or `Async.Scheduler` passed into `onComplete`.
This is because `Source` often represent values that come from *outside* the program's control, possibly running
on another thread pool. We do not want to introduce a "middleground" where spawning tasks is possible but will
implicitly live outside of structured concurrency.
With this limitation, we don't have control over how parallel or resource-intensive a `Source` would take
to call a `Listener`.

In Gears, we attempt to constrain this limitation by asking `Listener` implementations to be as resource-unintensive as possible.
For most cases, `Listener` involves just scheduling the resumption of the suspended `Async` context, so it is not a problem.

However, `transformValuesWith` allows one to easily run unlimited computation within the transform function `f`.
This is dangerous, so `transformValuesWith` has a long, tedious, _greppable_ name to show its gnar sides.

### I want to transform a `Source`, what do I do?

Explicitly spawn a Future that transforms each item and communicate with it through a `Channel`:

```scala
val ch = SyncChannel[U]()
Future:
  while true do
    val t = source.awaitResult
    ch.send(f(t))
```

This is not however not always what you want: think about whether you want `f` and `send` to be run at the same time,
or `awaitResult` and `send`, or what to do when `f` throws, ...
Most of the time this is very situation-specific, so Gears do not provide a simple solution.
