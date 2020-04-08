package eu.simonbinder.kart.kernel.binary.reader

import eu.simonbinder.kart.kernel.*
import eu.simonbinder.kart.kernel.DartVersion.Companion.SUPPORTED_KERNEL_VERSIONS
import eu.simonbinder.kart.kernel.ast.AsyncMarker
import eu.simonbinder.kart.kernel.ast.FunctionNode
import eu.simonbinder.kart.kernel.ast.expressions.Expression
import eu.simonbinder.kart.kernel.ast.expressions.InvalidExpression
import eu.simonbinder.kart.kernel.ast.expressions.VariableGet
import eu.simonbinder.kart.kernel.ast.expressions.VariableSet
import eu.simonbinder.kart.kernel.ast.members.*
import eu.simonbinder.kart.kernel.ast.statements.*
import eu.simonbinder.kart.kernel.binary.Tags
import eu.simonbinder.kart.kernel.types.*

class KernelReader(
    private val input: ByteArray
) {
    private var _offset: Int = 0
    private var offset: UInt
        get() = _offset.toUInt()
        set(value) { _offset = value.toInt() }

    private var currentKernelVersion = 0
    private val stringTable = mutableListOf<String>()
    private val nameTable = mutableListOf<CanonicalName>()
    private val sourceUriTable = mutableListOf<Uri>()

    private val variableStack = mutableListOf<VariableDeclaration>()

    private fun error(msg: String): Nothing {
        throw ParsingException(msg, offset)
    }

    private inline fun UIntArray.withNext(body: (Pair<UInt, UInt>) -> Unit) {
        asSequence().zipWithNext().forEach(body)
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

    private fun readStringReference() = stringTable[readUint().toInt()]

    private fun readCanonicalNameReference(): CanonicalName? {
        val biasedIndex = readUint().toInt()
        return if (biasedIndex == 0) null else nameTable[biasedIndex - 1]
    }

    private fun readReference() = readCanonicalNameReference()?.asReference()

    private fun readUriReference() = sourceUriTable[readUint().toInt()]

    private fun readFileOffset(): Int {
        return readUint().toInt() - 1
    }

    fun readComponent(): List<Component> {
        // We check magic and version at each component. But it can't hurt to check them on the beginning of the fail so
        // that we don't fail with cryptic errors when a user provides a non-Kernel file.
        if (readUint32() != Tags.MAGIC) error("Invalid Kernel file: Wrong magic bytes")

        val kernelVersion = readUint32().toInt()
        if (kernelVersion !in SUPPORTED_KERNEL_VERSIONS) {
            throw InvalidKernelVersionException(kernelVersion)
        }

        val indices = findComponentPositions()
        return indices.map(this::readOneComponent)
    }

    private fun readOneComponent(index: ComponentIndex): Component {
        val component = Component()
        component.nonNullableByDefaultCompiledMode = index.nnbdMode

        offset = index.startOffset

        // skip magic and format version, we already checked those
        if (readUint32() != Tags.MAGIC) error("Invalid component: Wrong magic bytes")
        currentKernelVersion = readUint32().toInt()
        if (currentKernelVersion !in SUPPORTED_KERNEL_VERSIONS) {
            throw InvalidKernelVersionException(currentKernelVersion)
        }

        // ignore prolemsAsJson as well
        readList { readString() }

        offset = index.offsetForStringTable
        readStringTable()

        offset = index.offsetForCanonicalNames
        val rootName = CanonicalName.root()
        readCanonicalNames(rootName)

        offset = index.offsetForSourceTable
        readSourceMap(component.sources)

        index.libraryOffsets.withNext { (start, end) ->
            offset = start
            component.libraries.add(readLibrary(end))
        }

        return component
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
        val endOffsets = readList(this::readUint).asSequence()
        val lengths = (sequenceOf(0u) + endOffsets).zipWithNext { a, b -> b - a }

        for (length in lengths) {
            stringTable.add(String(readByteArray(length)))
        }
    }

    private fun readCanonicalNames(root: CanonicalName) {
        val length = readUint().toInt()

        repeat(length) {
            val biasedParent = readUint().toInt()
            val name = readStringReference()

            val parent = if (biasedParent == 0) root else nameTable[biasedParent - 1]
            nameTable.add(parent.getChild(name))
        }
    }

    private fun readSourceMap(sources: MutableMap<Uri, Source>) {
        val length = readUint32().toInt()

        repeat(length) {
            val uriUtf8Bytes = readByteArray()
            val sourceUtfBytes = readByteArray()
            // skip line starts
            readList(this::readUint)
            val importUriUtf8Bytes = readByteArray()

            val sourceUri = Uri(String(uriUtf8Bytes))
            val info = Source(
                content = String(sourceUtfBytes),
                fileUri = sourceUri,
                importUri = Uri(String(importUriUtf8Bytes))
            )
            sources[sourceUri] = info
            sourceUriTable.add(sourceUri)
        }

        // We don't care about the sourceIndex field, so just skip it
        _offset += length * 4
    }

    private fun readLibrary(endOffset: UInt): Library {
        val savedOffset = offset

        // Read library index first
        offset = endOffset - 4u
        val procedureCount = readUint32()

        // go back to classCount, skipping procedureOffsets and procedureCount
        offset = endOffset - (procedureCount + 3u) * 4u
        val classCount = readUint32()
        val procedureOffsets = UIntArray(procedureCount.toInt() + 1) { readUint32() }

        // go back to classOffsets
        offset = endOffset - (procedureCount + classCount + 4u) * 4u
        val classOffsets = UIntArray(classCount.toInt() + 1) { readUint32() }

        offset = savedOffset

        val flags = readByte()
        val languageVersionMajor = readUint().toInt()
        val languageVersionMinor = readUint().toInt()

        val library = Library(reference = readReference()).apply {
            name = readStringReference()
            fileUri = readUriReference()
            this.flags = flags.toInt()
            dartVersion = DartVersion(languageVersionMajor, languageVersionMinor, currentKernelVersion)
        }

        procedureOffsets.withNext { (start, end) ->
            offset = start
            library.members.add(readProcedure(end))
        }

        return library
    }

    private fun readName(): Name {
        val name = readStringReference()
        return if (name.startsWith('_')) {
            Name(name, readReference())
        } else {
            Name(name)
        }
    }

    private fun readProcedure(endOffset: UInt): Procedure {
        val tag = readUint()
        assert(tag == Tags.PROCEDURE)

        val reference = readReference()
        val uri = readUriReference()
        val startFileOffset = readFileOffset()
        val fileOffset = readFileOffset()
        val fileEndOffset = readFileOffset()
        val kind = ProcedureKind.values()[readByte().toInt()]
        val flags = readUint().toInt()
        val name = readName()
        readList(this::readExpression) // todo annotations
        readReference() // forwardingStubSuperTarget, ignore
        readReference() // forwardingStubInterfaceTarget, ignore
        val function = readOption(this::readFunctionNode)

        return Procedure(kind, function, name, reference, uri).also {
            it.startFileOffset = startFileOffset
            it.fileOffset = fileOffset
            it.fileEndOffset = fileEndOffset
            it.flags = flags
        }
    }

    private inline fun <T> scoped(body: () -> T): T {
        val oldVariableStackSize = variableStack.size
        try {
            return body()
        } finally {
            val addedInBody = variableStack.size - oldVariableStackSize
            variableStack.dropLast(addedInBody)
        }
    }

    private fun readVariableReference() = variableStack[readUint().toInt()]

    private fun readFunctionNode(): FunctionNode {
        val tag = readUint()
        assert(tag == Tags.FUNCTION_NODE)

        return scoped {
            val offset = readFileOffset()
            val endOffset = readFileOffset()
            val asyncMarker = AsyncMarker.values()[readUint().toInt()]
            val dartAsyncMarker = AsyncMarker.values()[readUint().toInt()]

            val typeParameters = readList(this::readTypeParameter)
            readUint().toInt() // total parameter count. Not needed because they're lists
            val requiredParameterCount = readUint().toInt()
            val positionalParameters = readAndPushVariableDeclarations()
            val namedParameters = readAndPushVariableDeclarations()

            val returnType = readType()
            val body = readOption(this::readStatement)

            FunctionNode(
                typeParameters,
                positionalParameters,
                requiredParameterCount,
                namedParameters,
                returnType,
                asyncMarker,
                dartAsyncMarker,
                body
            ).also {
                it.fileOffset = offset
                it.endOffset = endOffset
            }
        }
    }

    private fun readStatements(): List<Statement> = readList(this::readStatement)

    private fun readStatement(): Statement = when (readByte()) {
        Tags.EXPRESSION_STATEMENT -> ExpressionStatement(readExpression())
        Tags.BLOCK -> scoped { Block(readStatements()) }
        Tags.EMPTY_STATEMENT -> EmptyStatement()
        else -> TODO()
    }

    private fun readAndPushVariableDeclarations(): List<VariableDeclaration> {
        return readList {
            val declaration = readVariableDeclaration()
            variableStack.add(declaration)
            declaration
        }
    }

    private fun readVariableDeclaration(): VariableDeclaration {
        TODO()
    }

    private fun readExpression(): Expression {
        val tag = readByte()

        return when(Tags.withoutSpecializedPayload(tag)) {
            Tags.INVALID_EXPRESSION -> {
                val fileOffset = readFileOffset()
                InvalidExpression(readStringReference()).also { it.fileOffset = fileOffset }
            }
            Tags.VARIABLE_GET -> {
                val fileOffset = readFileOffset()
                readUint() // offset for declaration in binary
                VariableGet(readVariableReference(), readOption(this::readType)).also { it.fileOffset = fileOffset }
            }
            Tags.SPECIALIZED_VARIABLE_GET -> {
                val fileOffset = readFileOffset()
                val variable = variableStack[Tags.specializedPayload(tag).toInt()]
                VariableGet(variable).also { it.fileOffset = fileOffset }
            }
            Tags.VARIABLE_SET -> {
                val offset = readFileOffset()
                readUint() // ignore declaration offset in binary
                VariableSet(readVariableReference(), readExpression()).also { it.fileOffset = offset }
            }
            Tags.SPECIALIZED_VARIABLE_SET -> {
                val fileOffset = readFileOffset()
                val variable = variableStack[Tags.specializedPayload(tag).toInt()]
                VariableSet(variable, readExpression()).also { it.fileOffset = fileOffset }
            }
            else -> TODO()
        }
    }

    private fun readTypeParameter(): TypeParameter {
        TODO()
    }

    private fun readType(): DartType {
        fun readNullability(): Nullability {
            return Nullability.values()[readByte().toInt()]
        }

        fun readTypes(): MutableList<DartType> = readList(this::readType).toMutableList()

        return when (readByte()) {
            Tags.BOTTOM_TYPE -> BottomType
            Tags.NEVER_TYPE -> NeverType(readNullability())
            Tags.INVALID_TYPE -> InvalidType
            Tags.DYNAMIC_TYPE -> DynamicType
            Tags.VOID_TYPE -> VoidType
            Tags.INTERFACE_TYPE -> InterfaceType(readNullability(), readReference(), readTypes())
            Tags.SIMPLE_INTERFACE_TYPE -> InterfaceType(readNullability(), readReference())
            else -> TODO()
        }
    }

}