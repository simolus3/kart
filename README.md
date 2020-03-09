# kart

Kart is a work-in-progress Kotlin to Dart compiler, aiming to support all platforms Dart runs on.

## Roadmap

1. Compile Kotlin: To get started, let's compile Kotlin code that doesn't have dependencies.
   We should support all statements and expressions, methods, classes and interfaces, etc.
   We'll support a tiny subset of Kotlin's standard library as compiler intrinsics.
2. Write a standard lib: Write a Kotlin stdlib implementation targeting Dart.
3. Further tooling support: Write a gradle plugin, look at Kotlin and Dart interop, incremental
compilation and hot reload, ...

## Development

### Setup

To start hacking on kart, clone this repository and download the [Kotlin CLI compiler](https://kotlinlang.org/docs/tutorials/command-line.html)
somewhere. Next, create a file called `local.properties` and put the following content into it:
```properties
kart.stdlib_jar=/path/to/downloaded/kotlinc/lib/kotlin-stdlib.jar
```

Finally, run `pub get` in the `kart_support` directory.

You can now compile a set of test cases with `./gradlew integration_tests:generateTextDescription`.

### Quickly compiling a file

Create a folder called `example` in this directory, then run `Main.kt` in `kotlin2kernel`, passing
the stdlib location from the setup step as parameter. This will compile all Kotlin sources in
`example` into `output.dill`, which can then be run in Dart.

### Viewing generated Dart

To view a generated `.dill` file in text form, you can run `dart tool/kernel_to_text.dart < file.dill`.
To run such file, just use `dart file.dill`.

### Project structure

For now, the project consists of two main modules: `kernel` is used to create `.dill` files in Kotlin.
`kotlin2kernel` is the main compiler, which transforms Kotlin IR to Kernel components.