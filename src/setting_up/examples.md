# Setting up gears

## Running Examples in this Book

The book contains some examples in the `src/scala` directory.
For example, this is the `hello` example, located in `src/scala/hello.scala`:

```scala 3
{{#include ../scala/hello.scala}}
```

All such examples can be run with [scala-cli](https://scala-cli.virtuslab.org)
provided the following dependencies:
- A locally published version of `gears`.
- Additional platform dependencies, as described below.

## Compiling `gears`

To compile `gears` from source, clone `gears` from the GitHub repository:
```bash
git clone https://github.com/lampepfl/gears --recursive
```

And run `rootJVM/publishLocal`.

### For Scala Native

Make sure to prepare additional [dependencies](https://scala-native.org/en/stable/user/setup.html) for Scala Native.
You would also need to locally publish a custom version of `munit` for Scala Native 0.5.0, which is available as a
`git` submodule from the gears repository.

To locally publish `munit`, from the root directory of `gears` repository:
```bash
git submodule update            # Create and update submodules
dependencies/publish_deps.sh # Locally publish pinned dependencies
```

Finally, `gears` artifacts for Scala Native can be locally published by
```bash
sbt rootNative/publishLocal
```

## Trying out examples

### On JVM

As mentioned in the introduction, we require a JVM version that supports virtual threads.
With that provided, examples can be run simply with
```bash
scala-cli run "path-to-example"
```
For the `hello` example:
```
scala-cli run src/scala/hello.scala
```

### On Scala Native

The current Scala Native version required by Gears is 0.5.1.
Therefore examples in this book hardcodes `0.5.1` as the Scala Native version, which
is also the version `gears` is currently compiled against.

Examples can be run with
```bash
scala-cli --platform scala-native run "path-to-example"
```
For the `hello` example:
```
scala-cli --platform scala-native run src/scala/hello.scala
```
