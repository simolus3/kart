package eu.simonbinder.kart.kernel.binary.reader

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
)