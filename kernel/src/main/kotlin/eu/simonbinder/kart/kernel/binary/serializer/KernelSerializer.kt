package eu.simonbinder.kart.kernel.binary.serializer

import eu.simonbinder.kart.kernel.*
import eu.simonbinder.kart.kernel.ast.FunctionNode
import eu.simonbinder.kart.kernel.ast.TreeNode
import eu.simonbinder.kart.kernel.ast.expressions.*
import eu.simonbinder.kart.kernel.ast.members.*
import eu.simonbinder.kart.kernel.ast.members.initializers.*
import eu.simonbinder.kart.kernel.ast.statements.*
import eu.simonbinder.kart.kernel.binary.Tags
import eu.simonbinder.kart.kernel.types.*
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.*
import kotlin.math.absoluteValue

/**
 * Serializes a Kernel [Component] into a `.dill` file.
 *
 * The resulting file requires further processing for it to be runnable. In particular, we need to link it to Dart's
 * standard library (like `vm_platform_strong.dill`). This process is currently implemented in a separate Dart library.
 *
 * https://github.com/dart-lang/sdk/blob/master/pkg/kernel/binary.md
 */
class KernelSerializer(
    output: OutputStream
): Visitor<Unit> {

    private val outputStream: DataOutputStream = DataOutputStream(output)

    private val stringIndexer = StringIndexer()
    private var variableIndexer: VariableIndexer? = null
    private val nameIndexer = NameIndexer()
    private val uriIndexer = UriIndexer()
    private var labelIndex: SimpleStackBasedIndexer<LabeledStatement>? = null

    private var binaryOffsetForComponent: UInt = 0u
    private var binaryOffsetForSourceTable: UInt = 0u
    private var binaryOffsetForCanonicalNames: UInt = 0u
    private var binaryOffsetForMetadataPayloads: UInt = 0u
    private var binaryOffsetForMetadataMappings: UInt = 0u
    private var binaryOffsetForStringTable: UInt = 0u
    private var binaryOffsetForConstantTable: UInt = 0u
    private val libraryOffsets = mutableListOf<UInt>()
    private val relevantNodeOffsets = WeakHashMap<TreeNode, UInt>()

    private val currentOffset get() = outputStream.size().toUInt()

    private fun useVariableIndexer(): VariableIndexer {
        if (variableIndexer == null) {
            variableIndexer = VariableIndexer()
        }
        return variableIndexer!!
    }

    private fun enterScope(memberScope: Boolean = false, variableScope: Boolean = false) {
        if (memberScope) {
            variableIndexer = null
        }

        if (variableScope) {
            if (variableIndexer == null) variableIndexer = VariableIndexer()
            variableIndexer!!.pushScope()
        }
    }

    private fun leaveScope(memberScope: Boolean = false, variableScope: Boolean = false) {
        if (memberScope) {
            variableIndexer = null
        }

        if (variableScope) {
            if (variableIndexer == null) variableIndexer = VariableIndexer()
            variableIndexer!!.popScope()
        }
    }

    private inline fun scoped(memberScope: Boolean = false, variableScope: Boolean = false, body: () -> Any?) {
        enterScope(memberScope, variableScope)
        body()
        leaveScope(memberScope, variableScope)
    }

    private fun writeByte(value: UInt) {
        assert(value.toUByte().toUInt() == value)
        outputStream.writeByte(value.toInt())
    }

    private fun writeBool(boolean: Boolean) {
        writeByte(if (boolean) 1u else 0u)
    }

    private fun writeUint7(value: UByte) {
        outputStream.writeByte(value.toInt() and 0b01111111) // 0xxxxxxx
    }

    private fun writeUint14(value: UInt) {
        val asInt = value.toInt()

        outputStream.writeByte((asInt shr 8) and 0b10111111 or 0b10000000) // 10xxxxxx
        outputStream.writeByte(asInt) // lower 8 bit
    }

    private fun writeUint30(value: UInt) {
        val asInt = value.toInt()

        outputStream.writeByte((asInt shr 16) or 0b11000000) // 11xxxxxx
        outputStream.writeByte(asInt shr 16)
        outputStream.writeByte(asInt shr 8)
        outputStream.writeByte(asInt)
    }

    private fun writeUint(value: UInt) {
        when {
            value < 1u shl 7 -> writeUint7(value.toUByte())
            value < 1u shl 14 -> writeUint14(value)
            else -> writeUint30(value)
        }
    }

    private fun writeUint32(value: UInt) {
        val asInt = value.toInt()

        outputStream.run {
            writeByte(asInt shr 24)
            writeByte(asInt shr 16)
            writeByte(asInt shr 8)
            writeByte(asInt)
        }
    }

    private fun writeFileOffset(value: Int) {
        writeUint((value + 1).toUInt())
    }

    private inline fun <T> writeList(list: Collection<T>, writer: (T) -> Unit) {
        writeUint(list.size.toUInt())
        list.forEach { writer(it) }
    }

    private inline fun <T> writeOption(value: T?, writer: (T) -> Unit) {
        if (value == null) {
            writeByte(Tags.NOTHING)
        } else {
            writeByte(Tags.SOMETHING)
            writer(value)
        }
    }

    private fun writeNodesAndCollectOffsets(nodes: Collection<Node>): UIntArray {
        writeUint(nodes.size.toUInt())

        val offsets = UIntArray(nodes.size + 1)
        nodes.forEachIndexed { index, node ->
            offsets[index] = currentOffset
            node.accept(this)
        }
        offsets[offsets.size - 1] = currentOffset
        return offsets
    }

    private fun writeOffsetsFollowedByCountAsUint32(offsets: UIntArray) {
        offsets.forEach(this::writeUint32)
        writeUint32(offsets.size.toUInt() - 1u)
    }

    private fun writeString(content: String) {
        val utf8Bytes = content.toByteArray(Charsets.UTF_8)
        writeUint(utf8Bytes.size.toUInt())
        outputStream.write(utf8Bytes)
    }

    private fun writeStringReference(content: String) {
        writeUint(stringIndexer.put(content).toUInt())
    }

    private fun writeCanonicalNameReference(name: CanonicalName?) {
        writeUint(nameIndexer[name].toUInt())
    }

    private fun writeName(name: Name) {
        name.accept(this)
    }

    private fun writeUriReference(uri: Uri?) {
        writeUint(uriIndexer.put(uri).toUInt())
    }

    private fun writeSourceMap(map: Map<Uri, Source>) {
        binaryOffsetForSourceTable = currentOffset

        val length = uriIndexer.length
        writeUint32(length.toUInt())

        val sourceIndex = UIntArray(length)

        for ((i, uri) in uriIndexer.keys.withIndex()) {
            var source = map[uri]

            if (source == null) {
                source = Source("fake content", null, null)
            }

            // write the source: uri bytes, source bytes, line starts, import uri bytes
            val uriString = source.fileUri?.content ?: ""
            val content = source.content

            sourceIndex[i] = currentOffset

            writeString(uriString)
            writeString(content)

            // Line starts are delta-encoded
            var previous = 0
            writeList(source.lineStarts) { start ->
                writeUint((start - previous).toUInt())
                previous = start
            }

            writeString(source.importUri?.content ?: "")
        }

        sourceIndex.forEach(this::writeUint32)
    }

    private fun writeStringTable() {
        binaryOffsetForStringTable = currentOffset
        val buffer = ByteArrayOutputStream()

        val endOffsets = UIntArray(stringIndexer.strings.size)
        var rollingLength = 0

        stringIndexer.strings.forEachIndexed { i, content ->
            val contentUtf8 = content.toByteArray(Charsets.UTF_8)
            rollingLength += contentUtf8.size

            buffer.write(contentUtf8)
            endOffsets[i] = rollingLength.toUInt()
        }

        writeList(endOffsets, this::writeUint)
        outputStream.write(buffer.toByteArray())
    }

    private fun writeReference(reference: Reference?) {
        writeCanonicalNameReference(reference?.canonicalName)
    }

    override fun defaultNode(node: Node) {
        throw NotImplementedError("Can't serialize $node")
    }

    fun writeComponent(node: Component) {
        visitComponent(node)
    }

    override fun visitComponent(node: Component) {
        binaryOffsetForComponent = currentOffset
        writeUint32(Tags.MAGIC)
        writeUint32(node.dartVersion.kernelVersion.toUInt())
        writeUint(0u) // skip problem list
        node.libraries.forEach { it.accept(this) }
        libraryOffsets.add(currentOffset)

        writeSourceMap(node.sources)
        binaryOffsetForCanonicalNames = currentOffset
        writeList(nameIndexer.listNames()) {
            writeCanonicalNameReference(it.parent)
            writeStringReference(it.name)
        }
        writeMetadataSection(node)
        writeStringTable()
        binaryOffsetForConstantTable = currentOffset
        writeUint32(0u) // no constants
        writeIndex(node)
    }

    private fun writeMetadataSection(component: Component) {
        // Note: The VM expects binaryOffsetsForMetadataPayloads to be word-aligned.
        // https://github.com/dart-lang/sdk/blob/684c53a6f174bcefd4e8202391a1a761abccead8/runtime/vm/kernel_loader.cc#L403
        val padding = 8 - (currentOffset.toInt() % 8)
        repeat(padding) { writeByte(0u) }

        binaryOffsetForMetadataPayloads = currentOffset

        // no metadata
        binaryOffsetForMetadataMappings = currentOffset
        writeUint32(0u) // no metadata mappings (it's a RList)
    }

    private fun writeIndex(node: Component) {
        // add padding so that the entire component is 8-byte aligned
        val writeNnbdMode = node.dartVersion.kernelVersion >= 41

        val elementsInIndex = (if (writeNnbdMode) 10 else 9) + libraryOffsets.size + 2 // amount of writeUint32
        val unalignedSize = currentOffset.toInt() + 4 * elementsInIndex
        val padding = 8 - (unalignedSize % 8)
        repeat(padding) {
            writeByte(0u)
        }

        writeUint32(binaryOffsetForSourceTable)
        writeUint32(binaryOffsetForCanonicalNames)
        writeUint32(binaryOffsetForMetadataPayloads)
        writeUint32(binaryOffsetForMetadataMappings)
        writeUint32(binaryOffsetForStringTable)
        writeUint32(binaryOffsetForConstantTable)
        writeUint32((nameIndexer[node.mainMethodReference?.canonicalName]).toUInt())
        if (node.dartVersion.kernelVersion >= 41) {
            writeUint32(node.nonNullableByDefaultCompiledMode.ordinal.toUInt())
        }
        libraryOffsets.forEach(this::writeUint32)
        writeUint32(node.libraries.size.toUInt())
        val size = currentOffset - binaryOffsetForComponent + 4u // +4 because we're about to write a 4 byte integer
        writeUint32(size)
    }

    override fun visitLibrary(node: Library) {
        libraryOffsets.add(currentOffset)
        writeByte(node.flags.toUInt())
        writeUint(node.dartVersion.major.toUInt())
        writeUint(node.dartVersion.minor.toUInt())
        writeCanonicalNameReference(node.canonicalName)
        writeStringReference(node.name ?: "")
        writeUriReference(node.fileUri)
        writeUint(0u) // no problems

        writeExpressions(node.annotations)
        writeUint(0u) // no library dependencies
        writeUint(0u) // no additional exports
        writeUint(0u) // no library parts
        writeUint(0u) // no typedefs
        val classOffsets = writeNodesAndCollectOffsets(node.classes)
        writeUint(0u) // no extensions
        writeList(node.fields) { field ->
            field.accept(this)
        }
        val procedureOffsets = writeNodesAndCollectOffsets(node.procedures)
        val sourceReferencesOffset = currentOffset
        writeList(node.sourceUris, this::writeUriReference)

        writeUint32(sourceReferencesOffset)
        writeOffsetsFollowedByCountAsUint32(classOffsets)
        writeOffsetsFollowedByCountAsUint32(procedureOffsets)
    }

    override fun visitClass(node: Class) {
        writeByte(Tags.CLASS)
        writeCanonicalNameReference(node.canonicalName)
        writeUriReference(node.fileUri)
        writeFileOffset(node.startFileOffset)
        writeFileOffset(node.fileOffset)
        writeFileOffset(node.fileEndOffset)
        writeByte(node.flags.toUInt())
        writeStringReference(node.name)
        writeExpressions(node.annotations)
        writeUint(0u) // empty type parameters list
        writeOption(node.superClass, this::writeType)
        writeOption(null) { throw AssertionError() } // mixedInType
        writeList(node.implementedClasses, this::writeType)

        writeList(node.fields) { it.accept(this) }
        writeList(node.constructors) { it.accept(this) }
        val procedureOffsets = writeNodesAndCollectOffsets(node.procedures)
        writeUint(0u) // redirecting factory constructors
        writeOffsetsFollowedByCountAsUint32(procedureOffsets)
    }

    override fun visitConstructor(node: Constructor) {
        scoped(variableScope = true) {
            writeByte(Tags.CONSTRUCTOR)
            writeCanonicalNameReference(node.canonicalName)
            writeUriReference(node.fileUri)
            writeFileOffset(node.fileStartOffset)
            writeFileOffset(node.fileOffset)
            writeFileOffset(node.fileEndOffset)
            writeByte(node.flags.toUInt())
            writeName(node.name)
            writeExpressions(node.annotations)
            visitFunctionNode(node.function)

            // Parameters are in scope in the initializers
            useVariableIndexer().restoreScope(node.function.totalParameterCount)
            writeList(node.initializers) { it.accept(this) }
        }
    }

    override fun visitField(node: Field) {
        scoped(memberScope = true) {
            writeByte(Tags.FIELD)
            writeCanonicalNameReference(node.canonicalName)
            writeUriReference(node.fileUri)
            writeFileOffset(node.fileOffset)
            writeFileOffset(node.fileEndOffset)
            writeUint(node.flags.toUInt()) // Note: Fields have more than 8 flags, so use uint instead of byte
            writeName(node.name)
            writeExpressions(node.annotations)
            writeType(node.type)
            writeOption(node.initializer, this::writeExpression)
        }
    }

    override fun visitProcedure(node: Procedure) {
        scoped(memberScope = true) {
            writeByte(Tags.PROCEDURE)
            writeCanonicalNameReference(node.canonicalName)
            writeUriReference(node.fileUri)
            writeFileOffset(node.startFileOffset)
            writeFileOffset(node.fileOffset)
            writeFileOffset(node.fileEndOffset)
            writeByte(node.kind.ordinal.toUInt())
            writeUint(node.flags.toUInt())
            visitName(node.name ?: emptyName)
            writeExpressions(node.annotations)
            writeReference(null) // forwarding stub super target reference
            writeReference(null) // forwarding stub interface target reference
            writeOption(node.function, this::visitFunctionNode)
        }
    }

    override fun visitName(node: Name) {
        writeStringReference(node.name)
        if (node.isPrivate) {
            writeReference(node.libraryReference)
        }
    }

    override fun visitFunctionNode(node: FunctionNode) {
        val oldLabelIndex = labelIndex

        writeByte(Tags.FUNCTION_NODE)
        enterScope(variableScope = true)

        writeFileOffset(node.fileOffset)
        writeFileOffset(node.endOffset)

        writeByte(node.asyncMarker.ordinal.toUInt()) // async marker
        writeByte(node.asyncMarker.ordinal.toUInt()) // dart async marker

        writeUint(0u) // type parameters
        val positionalCount = node.positionalParameters.size
        val namedCount = node.namedParameters.size
        writeUint((positionalCount + namedCount).toUInt())
        writeUint(node.requiredPositionalParameters.toUInt())

        writeList(node.positionalParameters, this::writeVariableDeclarationNoTag)
        writeList(node.namedParameters, this::writeVariableDeclarationNoTag)

        writeType(node.returnType)
        writeOption(node.body) { it.accept(this) }

        leaveScope(variableScope = true)

        labelIndex = oldLabelIndex
    }

    // Expressions

    private fun writeExpressions(expressions: Collection<Expression>) = writeList(expressions, this::writeExpression)

    private fun writeExpression(expression: Expression) {
        expression.accept(this)
    }

    private fun writeNamedExpression(namedExpr: NamedExpression) {
        namedExpr.accept(this)
    }

    override fun visitNamedExpression(node: NamedExpression) {
        // no tag on named expression
        writeStringReference(node.name)
        writeExpression(node.expression)
    }

    override fun visitStringLiteral(node: StringLiteral) {
        writeByte(Tags.STRING_LITERAL)
        writeStringReference(node.value)
    }

    override fun visitIntegerLiteral(node: IntegerLiteral) {
        val value = node.value

        if (value in -3..4) {
            // Write specialized literal
            writeByte(Tags.SPECIALIZED_INT_LITERAL + value.toUByte() + Tags.SPECIALIZED_INT_LITERAL_BIAS)
        } else if (value.absoluteValue shr 30 == 0L) {
            // fits into 30 bits, great
            if (value < 0) {
                writeByte(Tags.NEGATIVE_INT_LITERAL)
                writeUint30((-value).toUInt())
            } else {
                writeByte(Tags.POSITIVE_INT_LITERAL)
                writeUint30(value.toUInt())
            }
        } else {
            // yes, long literals are encoded as strings
            writeByte(Tags.BIG_INT_LITERAL)
            writeStringReference(value.toString())
        }
    }

    override fun visitDoubleLiteral(node: DoubleLiteral) {
        writeByte(Tags.DOUBLE_LITERAL)
        outputStream.writeDouble(node.value)
    }

    override fun visitBooleanLiteral(node: BooleanLiteral) {
        writeByte(if (node.value) Tags.TRUE_LITERAL else Tags.FALSE_LITERAL)
    }

    override fun visitNullLiteral(node: NullLiteral) {
        writeByte(Tags.NULL_LITERAL)
    }

    override fun visitNullCheck(node: NullCheck) {
        writeByte(Tags.NULL_CHECK)
        writeFileOffset(node.fileOffset)
        writeExpression(node.operand)
    }

    override fun visitAsExpression(node: AsExpression) {
        writeByte(Tags.AS_EXPRESSION)
        writeFileOffset(node.fileOffset)
        writeByte(node.flags.toUInt())
        writeExpression(node.operand)
        writeType(node.targetType)
    }

    override fun visitBlockExpression(node: BlockExpression) {
        scoped(variableScope = true) {
            writeByte(Tags.BLOCK_EXPRESSION)
            writeList(node.body, this::writeStatement)
            writeExpression(node.value)
        }
    }

    override fun visitConditionalExpression(node: ConditionalExpression) {
        writeByte(Tags.CONDITIONAL_EXPRESSION)
        writeExpression(node.condition)
        writeExpression(node.then)
        writeExpression(node.otherwise)
        writeOption(node.staticType, this::writeType)
    }

    override fun visitIsExpression(node: IsExpression) {
        writeByte(Tags.IS_EXPRESSION)
        writeFileOffset(node.fileOffset)
        writeByte(node.flags.toUInt())
        writeExpression(node.operand)
        writeType(node.targetType)
    }

    override fun visitNot(node: Not) {
        writeByte(Tags.NOT)
        writeExpression(node.operand)
    }

    override fun visitLogicalExpression(node: LogicalExpression) {
        writeByte(Tags.LOGICAL_EXPRESSION)
        writeExpression(node.left)
        writeByte(node.operator.ordinal.toUInt())
        writeExpression(node.right)
    }

    override fun visitInvalidExpression(node: InvalidExpression) {
        writeByte(Tags.INVALID_EXPRESSION)
        writeFileOffset(node.fileOffset)
        writeStringReference(node.message)
    }

    override fun visitStringConcatenation(node: StringConcatenation) {
        writeByte(Tags.STRING_CONCATENATION)
        writeFileOffset(node.fileOffset)
        writeList(node.expressions, this::writeExpression)
    }

    override fun visitThis(node: This) {
        writeByte(Tags.THIS)
    }

    override fun visitThrow(node: Throw) {
        writeByte(Tags.THROW)
        writeFileOffset(node.fileOffset)
        writeExpression(node.value)
    }

    override fun visitVariableGet(node: VariableGet) {
        val index = useVariableIndexer()[node.variable]!!.toUInt()
        val declarationPosition = relevantNodeOffsets[node.variable]!!

        if (index < 8u && node.promotedType == null) {
            // We can write a specialized variable get
            writeByte(Tags.SPECIALIZED_VARIABLE_GET + index)
            writeFileOffset(node.fileOffset)
            writeUint(declarationPosition)
        } else {
            writeByte(Tags.VARIABLE_GET)
            writeFileOffset(node.fileOffset)
            writeUint(declarationPosition)
            writeUint(index)
            writeOption(node.promotedType, this::writeType)
        }
    }

    override fun visitVariableSet(node: VariableSet) {
        val index = useVariableIndexer()[node.variable]!!.toUInt()
        val declarationPosition = relevantNodeOffsets[node.variable]!!

        if (index < 8u) {
            // use a specialized set
            writeByte(Tags.SPECIALIZED_VARIABLE_SET + index)
            writeFileOffset(node.fileOffset)
            writeUint(declarationPosition)
        } else {
            writeByte(Tags.VARIABLE_SET)
            writeFileOffset(node.fileOffset)
            writeUint(declarationPosition)
            writeUint(index)
        }
        writeExpression(node.value)
    }

    override fun visitStaticGet(node: StaticGet) {
        writeByte(Tags.STATIC_GET)
        writeFileOffset(node.fileOffset)
        writeReference(node.reference)
    }

    override fun visitStaticSet(node: StaticSet) {
        writeByte(Tags.STATIC_SET)
        writeFileOffset(node.fileOffset)
        writeReference(node.reference)
        writeExpression(node.value)
    }

    override fun visitStaticInvocation(node: StaticInvocation) {
        writeByte(Tags.STATIC_INVOCATION)
        writeFileOffset(node.fileOffset)
        writeReference(node.reference)
        writeArguments(node.arguments)
    }

    override fun visitPropertyGet(node: PropertyGet) {
        writeByte(Tags.PROPERTY_GET)
        writeFileOffset(node.fileOffset)
        writeExpression(node.receiver)
        writeName(node.name)
        writeReference(node.interfaceTarget)
    }

    override fun visitPropertySet(node: PropertySet) {
        writeByte(Tags.PROPERTY_SET)
        writeFileOffset(node.fileOffset)
        writeExpression(node.receiver)
        writeName(node.name)
        writeExpression(node.value)
        writeReference(node.interfaceTarget)
    }

    override fun visitMethodInvocation(node: MethodInvocation) {
        writeByte(Tags.METHOD_INVOCATION)
        writeFileOffset(node.fileOffset)
        writeExpression(node.receiver)
        writeName(node.name)
        writeArguments(node.arguments)
        writeReference(node.reference)
    }

    override fun visitConstructorInvocation(node: ConstructorInvocation) {
        writeByte(if (node.isConstant) Tags.CONST_CONSTRUCTOR_INVOCATION else Tags.CONSTRUCTOR_INVOCATION)
        writeFileOffset(node.fileOffset)
        writeReference(node.reference)
        writeArguments(node.arguments)
    }

    private fun writeArguments(args: Arguments) {
        args.accept(this)
    }

    override fun visitArguments(node: Arguments) {
        // note: There's no tag on arguments
        writeUint((node.positional.size + node.named.size).toUInt())
        writeList(node.typeParameters, this::writeType)
        writeList(node.positional, this::writeExpression)
        writeList(node.named, this::writeNamedExpression)
    }

    // Statements

    private fun writeStatement(stmt: Statement) {
        stmt.accept(this)
    }

    override fun visitBlock(node: Block) {
        scoped(variableScope = true) {
            writeByte(Tags.BLOCK)
            writeList(node.statements, this::writeStatement)
        }
    }

    override fun visitBreak(node: BreakStatement) {
        writeByte(Tags.BREAK_STATEMENT)
        writeFileOffset(node.fileOffset)
        writeUint(labelIndex!![node.to!!])
    }

    override fun visitEmptyStatement(node: EmptyStatement) {
        writeByte(Tags.EMPTY_STATEMENT)
    }

    override fun visitExpressionStatement(node: ExpressionStatement) {
        writeByte(Tags.EXPRESSION_STATEMENT)
        writeExpression(node.expression)
    }

    override fun visitIfStatement(node: IfStatement) {
        writeByte(Tags.IF_STATEMENT)
        writeFileOffset(node.fileOffset)
        writeExpression(node.condition)
        writeStatement(node.then)
        writeStatement(node.otherwise)
    }

    override fun visitLabeledStatement(node: LabeledStatement) {
        writeByte(Tags.LABELED_STATEMENT)
        labelIndex = (labelIndex ?: SimpleStackBasedIndexer())

        labelIndex!!.enter(node)
        writeStatement(node.body)
        labelIndex!!.exit()
    }

    override fun visitReturn(node: ReturnStatement) {
        writeByte(Tags.RETURN_STATEMENT)
        writeFileOffset(node.fileOffset)
        writeOption(node.expression, this::writeExpression)
    }

    override fun visitTryCatch(node: TryCatch) {
        node.computeNeedsStackTrace()

        writeByte(Tags.TRY_CATCH)
        writeStatement(node.body)
        writeByte(node.flags.toUInt())
        writeList(node.catches, this::writeCatch)
    }

    private fun writeCatch(node: Catch) {
        node.accept(this)
    }

    override fun visitCatch(node: Catch) {
        // Note: There is no tag on catch
        scoped(variableScope = true) {
            writeFileOffset(node.fileOffset)
            writeType(node.guard)
            writeOption(node.exception, this::writeVariableDeclarationNoTag)
            writeOption(node.stackTrace, this::writeVariableDeclarationNoTag)
            writeStatement(node.body)
        }
    }

    override fun visitTryFinally(node: TryFinally) {
        writeByte(Tags.TRY_FINALLY)
        writeStatement(node.body)
        writeStatement(node.finalizer)
    }

    override fun visitVariableDeclaration(node: VariableDeclaration) {
        writeByte(Tags.VARIABLE_DECLARATION)
        writeVariableDeclarationNoTag(node)
    }

    private fun writeVariableDeclarationNoTag(node: VariableDeclaration) {
        relevantNodeOffsets[node] = currentOffset
        writeFileOffset(node.fileOffset)
        writeFileOffset(node.fileEqualsOffset)
        writeExpressions(node.annotations)
        writeByte(node.flags.toUInt())
        writeStringReference(node.name ?: "")
        writeType(node.type)
        writeOption(node.initializer, this::writeExpression)
        // Variables are not in scope in their own initializer, so we're declaring it after writing the initializer
        useVariableIndexer().declare(node)
    }

    override fun visitDoWhile(node: DoStatement) {
        writeByte(Tags.DO_STATEMENT)
        writeFileOffset(node.fileOffset)
        writeStatement(node.body)
        writeExpression(node.condition)
    }

    override fun visitWhile(node: WhileStatement) {
        writeByte(Tags.WHILE_STATEMENT)
        writeFileOffset(node.fileOffset)
        writeExpression(node.condition)
        writeStatement(node.body)
    }

    // Initializers

    override fun visitFieldInitializer(node: FieldInitializer) {
        writeByte(Tags.FIELD_INITIALIZER)
        writeBool(node.isSynthetic)
        writeReference(node.field)
        writeExpression(node.value)
    }

    override fun visitSuperInitializer(node: SuperInitializer) {
        writeByte(Tags.SUPER_INITIALIZER)
        writeBaseRedirectingInitializer(node)
    }

    override fun visitRedirectingInitializer(node: RedirectingInitializer) {
        writeByte(Tags.REDIRECTING_INITIALIZER)
        writeBaseRedirectingInitializer(node)
    }

    private fun writeBaseRedirectingInitializer(node: BaseRedirectingInitializer) {
        writeBool(node.isSynthetic)
        writeFileOffset(node.fileOffset)
        writeReference(node.target)
        writeArguments(node.arguments)
    }

    // Types

    private fun writeType(node: DartType) {
        node.accept(this)
    }

    override fun visitBottomType(node: BottomType) {
        writeByte(Tags.BOTTOM_TYPE)
    }

    override fun visitInterfaceType(node: InterfaceType) {
        if (node.typeArguments.isEmpty()) {
            writeByte(Tags.SIMPLE_INTERFACE_TYPE)
            writeByte(node.nullability.value)
            writeReference(node.classReference)
        } else {
            writeByte(Tags.INTERFACE_TYPE)
            writeByte(node.nullability.value)
            writeReference(node.classReference)
            writeList(node.typeArguments, this::writeType)
        }
    }

    override fun visitNeverType(node: NeverType) {
        writeByte(Tags.NEVER_TYPE)
        writeByte(node.nullability.ordinal.toUInt())
    }

    override fun visitInvalidType(node: InvalidType) {
        writeByte(Tags.INVALID_TYPE)
    }

    override fun visitDynamicType(node: DynamicType) {
        writeByte(Tags.DYNAMIC_TYPE)
    }

    override fun visitVoidType(node: VoidType) {
        writeByte(Tags.VOID_TYPE)
    }

    private companion object {
        private val emptyName: Name = Name("")
    }
}