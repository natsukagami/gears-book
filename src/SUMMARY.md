# Summary

# Introduction

- [What is Gears?](./introduction.md)

# Starting up

- [Setting up Gears](./setting_up/examples.md)
- [Adding Gears in your own Scala Project](./setting_up/packages.md)

# Basic Concepts

- [An "Async" function](./basic/using_async.md)
- [Concurrency with Future](./basic/futures.md)
- [Groups, Scoping and Structured Concurrency](./basic/structured_concurrency.md)
- [Working with multiple Futures](./basic/future_await.md)
- [Supervising with Retries and Timeouts](./basic/retry.md)
- [Inter-future communication with Channels](./basic/channels.md)

# Dealing with Unstructured Concurrency

- [Sources as a Concurrency Primitive](./unstructured/sources.md)
- [Passive Futures and Promises](./unstructured/promises.md)
- [Select and Race](./unstructured/select_race.md)
- [Locking and Failible Listeners](./unstructured/listeners.md)
- [Cancellables and Scoping rules](./unstructured/cancellation.md)

# Writing Gears programs

- [Structured concurrency patterns](./idiomatic/spawn_and_await.md)
- [Avoid returning `Futures`](./idiomatic/avoid_futures.md)
- [`Async.blocking`: when to use](./idiomatic/blocking.md)
- [Staying referentially transparent with Async Blocks](./idiomatic/async_blocks.md)
- [Taking async lambdas as arguments](./idiomatic/async_args.md)

# Using Gears in your projects

- [Incremental adoption in your codebase](./usage/adopting-gears.md)

# Lower-level details

- [Async Support: Suspensions and Schedulers](./low-level/support.md)
- [Asynchronous Operations](./low-level/support.md)
- [Writing Cross-platform Gears libraries](./low-level/cross-platform.md)
