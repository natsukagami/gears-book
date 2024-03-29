# Inter-future communication with Channels

For immutable data, it is easy and recommended to directly
capture references to them from within `Future`s. However, to share mutable data
or to communicate between concurrent computations, Gears recommend following
Effective Go's [slogan on sharing](https://go.dev/doc/effective_go#sharing):
> Do not communicate by sharing memory; instead, share memory by communicating.

To facillitate this, Gears provides channels, a simple mean of pipe-style communication
between computations.

## Channels

[`Channel`](https://lampepfl.github.io/gears/api/gears/async/Channel.html) can be thought
of as a pipe between senders and readers, with or without a buffer in between. The latter
differentiates between the types of channels provided by Gears.
Nevertheless, all channels provide the following methods:
- [`send`](https://lampepfl.github.io/gears/api/gears/async/Channel.html#send-fffffa19) takes a value `T`
  and attempt to send it through the channel. Suspends until the value has been received or stored in
  the channel's buffer (if one exists).

  If the channel is closed, `send` will throw `ChannelClosedException`.
- [`read`](https://lampepfl.github.io/gears/api/gears/async/Channel.html#read-fffff569) suspends until
  a value is available through the channel, consuming it from the channel.

  `read` returns `Either[Closed, T]`, with `Left(Closed)` returned in the obvious case.
- [`close`](https://lampepfl.github.io/gears/api/gears/async/Channel.html#close-94c) simply closes the channel.

Channels provide the following guarantees:
- Two sequential `send`s (one `send` followed by another `send`, not done concurrently) will be `read` in the same order.
- An item that is `send` is always `read` exactly once, unless if it was in the channel's buffer when the channel is closed.

### Types of channels

Gears provide 3 types of channels:
- [`SyncChannel`](https://lampepfl.github.io/gears/api/gears/async/SyncChannel.html) are channels without a buffer,
  so every `send` will suspend until a corresponding `read` succeeds.

  Since there are no buffers, an item that is `send` is always `read` exactly once.
- [`BufferedChannel`](https://lampepfl.github.io/gears/api/gears/async/BufferedChannel.html) are channels with a buffer
  of a fixed size.
  `send`s will succeed immediately if there is space in the buffer, and suspend otherwise, until there is space for it.

  When `cancel`led, items in the buffer are dropped.
- [`UnboundedChannel`](https://lampepfl.github.io/gears/api/gears/async/UnboundedChannel.html) are channels with a buffer
  that is infinitely growable.
  `send`s always succeed immediately.
  In fact, `UnboundedChannel` provides [`sendImmediately`](https://lampepfl.github.io/gears/api/gears/async/UnboundedChannel.html#sendImmediately-fffff71f)
  that never suspends.

  When `cancel`led, items in the buffer are dropped.

### Restricted channel interfaces

When writing functions taking in channels, it is often useful to restrict them to be read-only or send-only.
Gears `Channel` trait is composed of
[`ReadableChannel`](https://lampepfl.github.io/gears/api/gears/async/ReadableChannel.html),
[`SendableChannel`](https://lampepfl.github.io/gears/api/gears/async/SendableChannel.html) and
`Closeable`, so any channel can be upcasted to one of the above three interfaces.

## `Async.select` with channels

`Async.select` can be used with channels. To do so, use
[`.readSource`](https://lampepfl.github.io/gears/api/gears/async/UnboundedChannel.html#sendImmediately-fffff71f) for reading and
[`.sendSource(T)`](https://lampepfl.github.io/gears/api/gears/async/Channel.html#sendSource-952) for sending; before
attaching `.handle` and providing a handler.

```scala
Async.blocking:
  val fut = Future:
    AsyncOperations.sleep(1.minute)
    10
  val ch = SyncChannel[Int]()
  val readFut = Future:
    ch.read().right.get + 1

  Async.select(
    fut.handle: x => println(s"fut returned $x"),
    ch.sendSource(20).handle:
      case Left(Closed) => throw Exception("channel closed!?")
      case Right(()) =>
        println(s"sent from channel!")
        println(s"readFut returned ${readFut.await}")
  )
```

Similar to future handlers, `Async.select` guarantees that exactly one of the handlers are run.
Not only so, it also guarantees that **only the channel event with the handler that is run** will go through!

For example, in the following snippet, it guarantees that exactly one channel is consumed from for each loop:
```scala
val chans = (1 to 2).map: i =>
  val chan = UnboundedChannel()
  chan.sendImmediately(i)
  chan

for i <- 1 to 2 do
  val read = Async.select(
    chans(0).readSource.handle: v => v,
    chans(1).readSource.handle: v => v,
  )
  println(read) // prints 1 2 or 2 1
```

It is possible to mix reads and sends within one `Async.select`, or mix channel operations with futures!

## Channel in Use: Actor pattern

One of the common patterns of using channels is to write functions that continuously take inputs from
one channel, process them, and send their output into another channel.
This is sometimes referred to as an Actor, or a Communicating Sequential Process.

Let's look at one of the examples,
inspired by [Rob Pike's talk on concurrency patterns in Go](https://go.dev/talks/2012/concurrency.slide#53).
This is an implementation of a concurrent [prime sieve](https://en.wikipedia.org/wiki/Sieve_of_Eratosthenes).

```scala
{{#include ../scala/prime_sieve.scala}}
```

Some notable points:
- `generate` and `sieve` are both `using Async` functions, they block the caller until their work has finished.
  We want to run them concurrently as actors, we spawn them with `Future.apply`.
- Both are given channels to write into, and the permission to close them. This is a common pattern, useful
  when your channels are *single sender, multiple readers*[^multiplexer].
  In such case, it is common for the sender to signal that there are no more inputs by simply closing the channel.
  All readers are then immediately notified, and in this case (where readers are senders themselves in a pipeline),
  they can forward this signal by closing their respective channels.

[^multiplexer]: in the case where multiple senders are involved, the logic is much more complicated. You might want
to look into [`ChannelMultiplexer`](https://lampepfl.github.io/gears/api/gears/async/ChannelMultiplexer.html).
