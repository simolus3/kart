package eu.simonbinder.kart.kernel.binary.reader

import eu.simonbinder.kart.kernel.ast.members.NonNullableByDefaultCompiledMode

internal class ComponentIndex(
    val startOffset: UInt,
    val offsetForSourceTable: UInt,
    val offsetForCanonicalNames: UInt,
    val offsetForMetadataPayloads: UInt,
    val offsetForMetadataMappings: UInt,
    val offsetForStringTable: UInt,
    val offsetForConstantTable: UInt,
    val mainMethodReference: UInt,
    val compilationMode: UInt,
    val libraryOffsets: UIntArray
) {

    val nnbdMode get() = NonNullableByDefaultCompiledMode.values()[compilationMode.toInt()]

}