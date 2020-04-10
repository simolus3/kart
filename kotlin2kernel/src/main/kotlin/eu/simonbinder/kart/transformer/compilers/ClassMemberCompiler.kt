package eu.simonbinder.kart.transformer.compilers

import eu.simonbinder.kart.kernel.asReference
import eu.simonbinder.kart.kernel.ast.expressions.Arguments
import eu.simonbinder.kart.kernel.ast.members.Constructor
import eu.simonbinder.kart.kernel.ast.members.initializers.FieldInitializer
import eu.simonbinder.kart.kernel.ast.members.initializers.Initializer
import eu.simonbinder.kart.kernel.ast.members.initializers.RedirectingInitializer
import eu.simonbinder.kart.kernel.ast.members.initializers.SuperInitializer
import eu.simonbinder.kart.transformer.context.InClassContext
import eu.simonbinder.kart.transformer.context.names
import eu.simonbinder.kart.transformer.isDartConstant
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.util.file

object ClassMemberCompiler : BaseMemberCompiler<InClassContext>() {

    override val isStatic: Boolean
        get() = false

    private val IrBody.statements: List<IrStatement> get() = when (this) {
        is IrExpressionBody -> listOf(expression)
        is IrBlockBody -> statements
        else -> error("Unknown body type")
    }

    override fun visitConstructor(declaration: IrConstructor, data: InClassContext) {
        // If the constructor calls another constructor (e.g. `constructor(): super()` or
        // via `: this()`, that would be represented in the IR body. But since those
        // things are supported in Dart directly, we try to reverse it.
        var body = declaration.body
        val statements = body?.statements ?: emptyList()

        // Transform the first statements to Kernel initializers, as long as that's possible.
        val initializers = mutableListOf<Initializer>()
        val bodyStatements = mutableListOf<IrStatement>()
        var isReadingInitializers = true

        val (context, parameters) = createBodyContextAndParams(declaration, data)

        for (stmt in statements) {
            if (!isReadingInitializers) {
                bodyStatements.add(stmt)
                continue
            }

            val initializer = when (stmt) {
                is IrDelegatingConstructorCall -> {
                    val isSuper = stmt.symbol.descriptor.constructedClass !=
                            declaration.symbol.descriptor.constructedClass

                    val target = if (data.info.dartIntrinsics.isDefaultObjectConstructor(stmt)) {
                        data.names.dartNames.objectDefaultConstructor.asReference()
                    } else {
                        data.names.nameFor(stmt.symbol.owner)
                    }

                    val arguments = Arguments(
                        positional = List(stmt.valueArgumentsCount) { index ->
                            stmt.getValueArgument(index)!!.accept(ExpressionCompiler, context)
                        }
                    )

                    if (isSuper) {
                        SuperInitializer(target, arguments)
                    }  else {
                        RedirectingInitializer(target, arguments)
                    }
                }
                is IrSetField -> {
                    val dartField = data.names.nameFor(stmt.symbol.owner)
                    val value = stmt.value.accept(ExpressionCompiler, context)

                    FieldInitializer(dartField, value)
                }
                else -> null
            }

            if (initializer != null) {
                initializers.add(initializer)
            } else {
                isReadingInitializers = false
                bodyStatements.add(stmt)
            }
        }

        body = body?.run { IrBlockBodyImpl(startOffset, endOffset, bodyStatements) }

        val dartConstructor = Constructor(
            reference = data.names.nameFor(declaration),
            name = data.names.simpleNameFor(declaration),
            fileUri = data.info.loadFile(declaration.file),
            function = compileFunctionNodeWithContextAndParameters(declaration, context, parameters, body)
        )
        initializers.forEach { dartConstructor.initializers.add(it) }
        dartConstructor.isConst = declaration.isDartConstant()

        data.target.members.add(dartConstructor)
    }
}