# Cancellables and Scoping rules

In the section about [Structured Concurrency](../basic/structured_concurrency.md), we learned that
`Async` contexts can manage linked `Future`s.
Let's explore this concept in details in this section.

## `CompletionGroup`

Every `Async` context created from all sources (`Async.blocking`, `Async.group` and `Future.apply`) creates a brand new
`CompletionGroup`, which is a set of `Cancellable`s.
Upon the end of the `Async` context's body, this `CompletionGroup` handles calling `.cancel` on all its `Cancellable`s
and wait for all of them to be removed from the group.

Adding a `Cancellable` to a `Group` is done by calling
[`add`](https://lampepfl.github.io/gears/api/gears/async/CompletionGroup.html#add-b9c) on the `Group`,
or calling [`link`](https://lampepfl.github.io/gears/api/gears/async/CompletionGroup.html#link-fffff3a9)
on the `Cancellable`.
Since every `Async` context contains a `CompletionGroup`,
an overload of [`link`](https://lampepfl.github.io/gears/api/gears/async/CompletionGroup.html#link-10) exists
that would add the `Cancellable` to the `Async` context's group.

## `Async`-scoped values

If you have data that needs to be cleaned up when the `Async` scope ends, implement `Cancellable` and `link` the data
to the `Async` scope:

```scala
case class CloseCancellable(c: Closeable) extends Cancellable:
  def cancel() =
    c.close()
    unlink()

extension (closeable: Closeable)
  def link()(using Async) =
    CloseCancellable(closeable).link()
```

However, note that this would possibly create *dangling* resources that links to the *passed-in* `Async` context
of a function:

```scala
def f()(using Async) =
  def resource = new Resource()
  resource.link() // links to Async

def main() =
  Async.blocking:
    f()
    // resource is still alive
    g()
    // ...
    0
    // resource is cancelled *here*
```

## Unlinking

Just as `Cancellable`s can be `link`ed to a group, you can also `unlink` them from their current group.
This is one way to create a *dangling* active `Future` that is indistinguishable from a passive `Future`.
(Needless to say this is **very much not recommended**).

To write a function that creates and returns an active `Future`, write a function that takes an `Async.Spawn` context:

```scala
def returnsDangling()(using Async.Spawn): Future[Int] =
  Future:
    longComputation()
```

Again, this is a pattern that *breaks* structured concurrency and should not be used carelessly!
One should _avoid_ exposing this pattern of functions in a Gears-using public API of a library.
