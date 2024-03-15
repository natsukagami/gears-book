# Supervising with Retries and Timeouts

With the ability to _sleep_ non-blockingly, one might immediately think about implementing
supervision of tasks with retrying behavior upon failure.

Gears provide some building blocks, as well as interfaces to customize the retry policies.

## Timeout

The most simple supervision function is [`withTimeout`](https://lampepfl.github.io/gears/api/gears/async.html#withTimeout-fffffdf9),
which takes a duration and an _async block_ (`Async ?=> T`). It runs the async block racing with the timeout, and
throws `TimeoutException` if the timeout runs out before the async block, cancelling the
async block afterwards.

```scala
Async.blocking:
  withTimeout(60.seconds):
    val body = request.get("https://google.com")
    // ...
```
[`withTimeoutOption`](https://lampepfl.github.io/gears/api/gears/async.html#withTimeoutOption-fffffda3)
is a variant that wraps the output in `Option[T]` instead, returning `None` on timeout.

## Retry

Similar to `withTimeout`, [`Retry`](https://lampepfl.github.io/gears/api/gears/async/Retry.html#)
takes a block and executes it, "blocking" the caller until its policy has finished.

`Retry` allows you to specify:
- when to consider the block as complete (until success, until failure, never)
- how many consecutive failures should fail the policy
- how much to delay in between attempts through the [`Delay`](https://lampepfl.github.io/gears/api/gears/async/Retry$$Delay.html) trait
  - Some delay policies are provided, such as a constant delay and an exponential backoff algorithm.

```scala
Async.blocking:
  Retry
    .untilSuccess
    .withMaximumFailures(5)
    .withDelay(Delay.exponentialBackoff(maximum = 1.minute, starting = 1.second, jitter = Jitter.full)):
      val body = request.get("https://google.com")
      // ...
```
