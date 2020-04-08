package eu.simonbinder.kart.kernel.binary.reader

import eu.simonbinder.kart.kernel.DartVersion.Companion.SUPPORTED_KERNEL_VERSIONS
import eu.simonbinder.kart.kernel.ast.members.Component
import eu.simonbinder.kart.kernel.binary.Tags

class KernelReader(
    private val input: ByteArray
) {
    private var _offset: Int = 0
    private var offset: UInt
        get() = _offset.toUInt()
        set(value) { _offset = value.toInt() }

    private val stringTable = mutableListOf<String>()

    private fun error(msg: String): Nothing {
        throw ParsingException(msg, offset)
    }

    private fun readByte(): UInt {
        return input[_offset++].toUByte().toUInt()
    }

    private fun readBoolean(): Boolean = when (readByte()) {
        0u -> false
        1u -> true
        else -> error("Unexpected boolean value")
    }

    private fun readUint(): UInt {
        val byte = readByte()

        return when {
            byte and 0x80u == 0u -> byte // 0xxxxxxx
            byte and 0x40u == 0u -> {
                // 10xxxxxx xxxxxxxx
                return (byte and 0x3Fu) or readByte()
            }
            else -> {
                // 11xxxxxx xxxxxxxx
                return (byte and 0x3Fu) or
                        (readByte() shr 16) or
                        (readByte() shr 8) or
                        readByte()
            }
        }
    }

    private fun readUint32(): UInt {
        return (readByte() shl 24) or (readByte() shl 16) or (readByte() shl 8) or readByte()
    }

    private fun readDouble(): Double {
        val data = (readUint32().toLong() shl 32) or readUint32().toLong()
        return Double.fromBits(data)
    }

    private inline fun <T> readList(readElement: () -> T): List<T> = List(readUint().toInt()) { readElement() }

    private inline fun <T: Any> readOption(readElement: () -> T): T? = if (readBoolean()) readElement() else null

    private fun readByteArray(length: UInt): ByteArray {
        val startOffset = _offset
        offset += length
        return input.sliceArray(startOffset until _offset)
    }
    private fun readByteArray(): ByteArray = readByteArray(readUint())

    private fun readString(): String = String(readByteArray())

    fun readComponent(): List<Component> {
        if (readUint32() != Tags.MAGIC) error("Invalid Kernel file: Wrong magic bytes")

        val kernelVersion = readUint32().toInt()
        if (kernelVersion !in SUPPORTED_KERNEL_VERSIONS) {
            throw InvalidKernelVersionException(kernelVersion)
        }

        val indices = findComponentPositions()
        return indices.map(this::readOneComponent)
    }

    private fun readOneComponent(index: ComponentIndex): Component {
        offset = index.startOffset

        // skip magic and format version, we already checked those
        readUint32()
        readUint32()

        // ignore prolemsAsJson as well
        readList { readString() }

        offset = index.offsetForStringTable
        readStringTable()

        TODO()
    }

    private fun findComponentPositions(): List<ComponentIndex> {
        val savedOffset = offset
        val indices = mutableListOf<ComponentIndex>()

        // Dill files may contain multiple components (concatenated together). All of them end with a ComponentIndex.
        // We read the last one, find the start offset and so on.
        _offset = input.size - 8

        while (offset > 0u) {
            val libraryCount = readUint32()
            val componentFileSizeInBytes = readUint32()

            val startOffset = offset - componentFileSizeInBytes

            // go to start of ComponentIndex, which consists of (11 + libraryOffsets) int32 values
            offset -= 4u * (11u + libraryCount)

            indices.add(ComponentIndex(
                startOffset = startOffset,
                offsetForSourceTable = readUint32(),
                offsetForCanonicalNames = readUint32(),
                offsetForMetadataPayloads = readUint32(),
                offsetForMetadataMappings = readUint32(),
                offsetForStringTable = readUint32(),
                offsetForConstantTable = readUint32(),
                mainMethodReference = readUint32(),
                compilationMode = readUint32(),
                libraryOffsets = UIntArray(libraryCount.toInt() + 1) { readUint32() }
            ))

            offset = startOffset
        }

        offset = savedOffset
        return indices
    }

    private fun readStringTable() {
        // List<UInt> endOffsets followed by byte[endOffsets.last] utf8Bytes
        val lengths = readList(this::readUint).zipWithNext { a, b -> b - a }

        for (length in lengths) {
            stringTable.add(String(readByteArray(length)))
        }
    }
}