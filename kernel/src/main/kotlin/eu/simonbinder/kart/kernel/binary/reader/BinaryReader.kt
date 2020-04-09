package eu.simonbinder.kart.kernel.binary.reader

import eu.simonbinder.kart.kernel.*
import eu.simonbinder.kart.kernel.DartVersion.Companion.SUPPORTED_KERNEL_VERSIONS
import eu.simonbinder.kart.kernel.ast.AsyncMarker
import eu.simonbinder.kart.kernel.ast.FunctionNode
import eu.simonbinder.kart.kernel.ast.expressions.*
import eu.simonbinder.kart.kernel.ast.members.*
import eu.simonbinder.kart.kernel.ast.members.initializers.FieldInitializer
import eu.simonbinder.kart.kernel.ast.members.initializers.Initializer
import eu.simonbinder.kart.kernel.ast.members.initializers.RedirectingInitializer
import eu.simonbinder.kart.kernel.ast.members.initializers.SuperInitializer
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
    private val typeParameterStack = mutableListOf<TypeParameter>()
    private val labelStack = mutableListOf<LabeledStatement>()

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

    private fun readUint32Array(length: Int) = UIntArray(length) { readUint32() }

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
        val procedureOffsets = readUint32Array(procedureCount.toInt() + 1)

        // go back to classOffsets
        offset = endOffset - (procedureCount + classCount + 4u) * 4u
        val classOffsets = readUint32Array(classCount.toInt() + 1)

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

        classOffsets.withNext { (start, end) ->
            offset = start
            library.classes.add(readClass(end))
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

    private fun readClass(classEndOffset: UInt): Class {
        val tag = readUint()
        assert(tag == Tags.CLASS)

        // Read index at the end of the class section
        val currentOffset = offset
        offset = classEndOffset - 4u
        val procedureCount = readUint32()
        offset = classEndOffset - (procedureCount + 2u) * 4u
        val procedureOffsets = readUint32Array(procedureCount.toInt() + 1)
        offset = currentOffset

        return typeParameterScope {
            val reference = readReference()
            val fileUri = readUriReference()
            val startOffset = readFileOffset()
            val fileOffset = readFileOffset()
            val endOffset = readFileOffset()
            val flags = readByte().toInt()
            val name = readStringReference()
            val annotations = readExpressions()
            readAndPushTypeParameters() // todo type parameters
            val superClass = readOption(this::readType)
            readOption(this::readType) // skip mixedInType
            val implementedClasses = readTypes().toMutableList()

            Class(reference, name, fileUri, superClass, implementedClasses).also {
                it.startFileOffset = startOffset
                it.fileOffset = fileOffset
                it.fileEndOffset = endOffset
                it.flags = flags
                it.annotations += annotations

                readList(this::readField).forEach { field -> it.members += field }
                readList(this::readConstructor).forEach { constructor -> it.members += constructor }

                procedureOffsets.withNext { (start, end) ->
                    offset = start
                    it.members.add(readProcedure(end))
                }
            }
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
        val annotations = readExpressions()
        readReference() // forwardingStubSuperTarget, ignore
        readReference() // forwardingStubInterfaceTarget, ignore
        val function = readOption(this::readFunctionNode)

        return Procedure(kind, function, name, reference, uri).also {
            it.startFileOffset = startFileOffset
            it.fileOffset = fileOffset
            it.fileEndOffset = fileEndOffset
            it.flags = flags
            it.annotations += annotations
        }
    }

    private fun readField(): Field {
         val tag = readUint()
        assert(tag == Tags.FIELD)

        val reference = readReference()
        val uri = readUriReference()
        val fileOffset = readFileOffset()
        val fileEndOffset = readFileOffset()
        val flags = readUint().toInt()
        val name = readName()
        val annotations = readExpressions()
        val type = readType()
        val initializer = readOption(this::readExpression)

        return Field(name, reference, type, initializer, uri).also {
            it.fileOffset = fileOffset
            it.fileEndOffset = fileEndOffset
            it.flags = flags
            it.annotations += annotations
        }
    }

    private fun readConstructor(): Constructor {
        val tag = readUint()
        assert(tag == Tags.CONSTRUCTOR)

        val reference = readReference()
        val fileUri = readUriReference()
        val fileStartOffset = readFileOffset()
        val fileOffset = readFileOffset()
        val fileEndOffset = readFileOffset()
        val flags = readByte().toInt()
        val name = readName()
        val annotations = readExpressions()
        val function = readFunctionNode()
        val initializers = readList(this::readInitializer)

        return Constructor(reference, name, fileUri, function).also {
            it.fileStartOffset = fileStartOffset
            it.fileOffset = fileOffset
            it.fileEndOffset = fileEndOffset
            it.flags = flags
            it.initializers += initializers
            it.annotations += annotations
        }
    }

    private inline fun <T> scopedImpl(stack: MutableList<out Any?>, crossinline body: () -> T): T {
        val oldStackSize = stack.size

        val result = body()

        val addedInBody = stack.size - oldStackSize
        stack.dropLast(addedInBody)

        return result
    }

    private inline fun <T> variableScope(crossinline body: () -> T): T = scopedImpl(variableStack, body)
    private inline fun <T> typeParameterScope(crossinline body: () -> T): T = scopedImpl(typeParameterStack, body)
    private inline fun <T> variableAndTypeParameterScope(crossinline body: () -> T): T {
        return scopedImpl(variableStack) { scopedImpl(typeParameterStack, body) }
    }

    private fun readVariableReference() = variableStack[readUint().toInt()]

    private fun readFunctionNode(): FunctionNode {
        val tag = readUint()
        assert(tag == Tags.FUNCTION_NODE)

        return variableAndTypeParameterScope {
            val offset = readFileOffset()
            val endOffset = readFileOffset()
            val asyncMarker = AsyncMarker.values()[readUint().toInt()]
            val dartAsyncMarker = AsyncMarker.values()[readUint().toInt()]

            val typeParameters = readAndPushTypeParameters()
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

    private fun readStatement(): Statement = when (val tag = readByte()) {
        Tags.EXPRESSION_STATEMENT -> ExpressionStatement(readExpression())
        Tags.BLOCK -> variableScope { Block(readStatements()) }
        Tags.EMPTY_STATEMENT -> EmptyStatement()
        Tags.LABELED_STATEMENT -> {
            val label = LabeledStatement().also { labelStack.add(it) }
            label.body = readStatement()
            labelStack.removeLast()
            label
        }
        Tags.BREAK_STATEMENT -> {
            val fileOffset = readFileOffset()
            BreakStatement(labelStack[readUint().toInt()]).also { it.fileOffset = fileOffset }
        }
        Tags.WHILE_STATEMENT -> {
            val fileOffset = readFileOffset()
            WhileStatement(readExpression(), readStatement()).also { it.fileOffset = fileOffset }
        }
        Tags.DO_STATEMENT -> {
            val fileOffset = readFileOffset()
            val body = readStatement()
            DoStatement(readExpression(), body).also { it.fileOffset = fileOffset }
        }
        Tags.IF_STATEMENT -> {
            val fileOffset = readFileOffset()
            IfStatement(readExpression(), readStatement(), readStatement()).also { it.fileOffset = fileOffset }
        }
        Tags.RETURN_STATEMENT -> {
            val fileOffset = readFileOffset()
            ReturnStatement(readOption(this::readExpression)).also { it.fileOffset = fileOffset }
        }
        Tags.TRY_CATCH -> {
            TryCatch(readStatement()).also {
                it.flags = readByte().toInt()
                it.catches += readList(this::readCatch)
            }
        }
        Tags.VARIABLE_DECLARATION -> readAndPushVariableDeclaration()
        else -> error("Unexpected statement tag: $tag")
    }

    private fun readCatch(): Catch {
        val fileOffset = readFileOffset()
        val guard = readType()
        val exception = readOption { readAndPushVariableDeclaration() }
        val stackTrace = readOption { readAndPushVariableDeclaration() }
        val body = readStatement()

        return Catch(guard, exception, stackTrace, body).also { it.fileOffset = fileOffset }
    }

    private fun readAndPushVariableDeclarations(): List<VariableDeclaration> {
        return readList(this::readAndPushVariableDeclaration)
    }

    private fun readAndPushVariableDeclaration(): VariableDeclaration {
        return readVariableDeclaration().also {
            variableStack.add(it)
        }
    }

    private fun readVariableDeclaration(): VariableDeclaration {
        val fileOffset = readFileOffset()
        val fileEqualsOffset = readFileOffset()
        val annotations = readExpressions()
        val flags = readByte().toInt()
        val name = readStringReference()
        val type = readType()
        val initializer = readOption(this::readExpression)

        return VariableDeclaration(name, type, initializer).also {
            it.fileOffset = fileOffset
            it.fileEqualsOffset = fileEqualsOffset
            it.flags = flags
            it.annotations += annotations
        }
    }

    private fun readExpressions(): List<Expression> {
        return readList(this::readExpression)
    }

    private fun readNamedExpression(): NamedExpression {
        return NamedExpression(readStringReference(), readExpression())
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
                readUint() // offset for declaration in binary
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
            Tags.METHOD_INVOCATION -> {
                val fileOffset = readFileOffset()
                MethodInvocation(readExpression(), readName(), readArguments(), readReference()).also {
                    it.fileOffset = fileOffset
                }
            }
            Tags.STATIC_INVOCATION -> {
                val fileOffset = readFileOffset()
                StaticInvocation(readReference()!!, readArguments()).also { it.fileOffset = fileOffset }
            }
            Tags.CONSTRUCTOR_INVOCATION -> {
                val fileOffset = readFileOffset()
                ConstructorInvocation(readReference(), readArguments()).also { it.fileOffset = fileOffset }
            }
            Tags.NOT -> Not(readExpression())
            Tags.NULL_CHECK -> {
                val fileOffset = readFileOffset()
                NullCheck(readExpression()).also { it.fileOffset = fileOffset }
            }
            Tags.LOGICAL_EXPRESSION -> {
                val left = readExpression()
                val operator = if (readBoolean()) LogicalOperator.AND else LogicalOperator.OR
                LogicalExpression(operator, left, readExpression())
            }
            Tags.CONDITIONAL_EXPRESSION -> ConditionalExpression(readExpression(), readExpression(), readExpression()).also {
                it.staticType = readOption(this::readType)
            }
            Tags.STRING_CONCATENATION -> {
                val fileOffset = readFileOffset()
                StringConcatenation(readExpressions()).also { it.fileOffset = fileOffset }
            }
            Tags.STRING_LITERAL -> StringLiteral(readStringReference())
            Tags.SPECIALIZED_INT_LITERAL -> IntegerLiteral(Tags.unbiasedSpecializedPayload(tag).toLong())
            Tags.POSITIVE_INT_LITERAL -> IntegerLiteral(readUint().toLong())
            Tags.NEGATIVE_INT_LITERAL -> IntegerLiteral(-readUint().toLong())
            Tags.BIG_INT_LITERAL -> IntegerLiteral(readStringReference().toLong())
            Tags.DOUBLE_LITERAL -> DoubleLiteral(readDouble())
            Tags.TRUE_LITERAL -> BooleanLiteral(true)
            Tags.FALSE_LITERAL -> BooleanLiteral(false)
            Tags.NULL_LITERAL -> NullLiteral()
            Tags.THIS -> This
            Tags.THROW -> {
                val fileOffset = readFileOffset()
                Throw(readExpression()).also { it.fileOffset = fileOffset }
            }
            Tags.BLOCK_EXPRESSION -> BlockExpression(readStatements(), readExpression())
            else -> error("Unexpected expression tag: $tag")
        }
    }

    private fun readArguments(): Arguments {
        readUint() // positional.length + named.length
        val typeParameters = readTypes()
        val positional = readExpressions()
        val named = readList(this::readNamedExpression)

        return Arguments(typeParameters, positional, named)
    }

    private fun readAndPushTypeParameters(): List<TypeParameter> {
        return readList {
            readTypeParameter().also { typeParameterStack.add(it) }
        }
    }

    private fun readTypeParameter(): TypeParameter {
        TODO()
    }

    private fun readTypes(): List<DartType> = readList(this::readType)

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

    private fun readInitializer(): Initializer {
        val tag = readByte()
        val isSynthetic = readBoolean()

        return when (tag) {
            Tags.FIELD_INITIALIZER -> FieldInitializer(readReference(), readExpression())
            Tags.SUPER_INITIALIZER -> {
                val fileOffset = readFileOffset()
                SuperInitializer(readReference(), readArguments()).also { it.fileOffset = fileOffset }
            }
            Tags.REDIRECTING_INITIALIZER -> {
                val fileOffset = readFileOffset()
                RedirectingInitializer(readReference(), readArguments()).also { it.fileOffset = fileOffset }
            }
            else -> TODO()
        }.also { initializer -> initializer.isSynthetic = isSynthetic }
    }

}