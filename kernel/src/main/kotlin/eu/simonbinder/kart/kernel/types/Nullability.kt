package eu.simonbinder.kart.kernel.types

enum class Nullability(val value: UInt) {

    // Note that the kernel binary spec appears to be wrong here. We use
    // https://github.com/dart-lang/sdk/blob/fbe9f6115d2fac78c5dd4a044b34e2d493edd32c/pkg/kernel/lib/ast.dart#L5680

    NULLABLE(1u),
    NON_NULLABLE(2u),
    NEITHER(0u),
    LEGACY(3u)
}