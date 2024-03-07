# Introduction to Gears

This is a book about [Gears](https://github.com/lampepfl/gears), an experimental asynchronous programming library for Scala 3!

Gears aim to provide a basis for writing **cross-platform** high-level asynchronous code with **direct-style Scala** and **structured concurrency**,
while allowing library implementations to deal with external asynchronous IO with featureful primitives and expose a simple direct-style API.

While Gears is currently in experimental stage (definitely not recommended for production!), we provide basic support for
- Virtual-threads-enabled JVM environments (JRE 21 or later, or JRE 19 with experimental virtual threads enabled)
- [Scala Native](https://scala-native.org) 0.5.0 or later with *delimited continuations* support (on Linux, MacOS and BSDs).

**Note**: This book is currently tracking **Gears 0.2.0 snapshot**, which has some difference in design from the previous 0.1 released version.
