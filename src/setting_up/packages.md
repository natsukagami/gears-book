# Adding Gears in your own Scala Project

Gears can be added to your own project by adding the following dependency,
by your preferred build tool:

- **With `sbt`**:
  ```scala
  libraryDependencies += "ch.epfl.lamp" %% "gears" % "0.2.0",
  ```
- **With `mill`**:
  ```scala
  def ivyDeps = Agg(
    // ... other dependencies
    ivy"ch.epfl.lamp::gears::0.2.0"
  )
  ```
- **With `scala-cli`**:
  ```scala
  //> using dep "ch.epfl.lamp::gears::0.2.0"
  ```
