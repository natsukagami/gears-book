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

Gears binaries are not currently published, as they depend on unreleased versions of Scala Native.
This means that we need to manually compile and locally publish it.

To do so, clone `gears` from the GitHub repository:
```bash
git clone https://github.com/lampepfl/gears
```

And run `rootJVM/publishLocal`.

### For Scala Native

Make sure to prepare additional [dependencies](https://scala-native.org/en/stable/user/setup.html) for Scala Native.
You would also need to locally publish a custom version of `munit` for Scala Native 0.5.0, which is available as a
`git` submodule from the gears repository.

To locally publish `munit`, from the root directory of `gears` repository:
```bash
git module update            # Create and update submodules
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

The current Scala Native version required by Gears is 0.5.0, which is unreleased.
Therefore examples in this book hardcodes `0.5.0-RC1` as the Scala Native version, which
is also the version `gears` is currently compiled against.

Examples can be run with
```bash
scala-cli --platform scala-native run "path-to-example"
```
For the `hello` example:
```
scala-cli --platform scala-native run src/scala/hello.scala
```
