package eu.simonbinder.kart.transformer.compilers

import eu.simonbinder.kart.kernel.Name
import eu.simonbinder.kart.kernel.asReference
import eu.simonbinder.kart.kernel.expressions.Arguments
import eu.simonbinder.kart.kernel.members.Constructor
import eu.simonbinder.kart.kernel.members.initializers.RedirectingInitializer
import eu.simonbinder.kart.kernel.members.initializers.SuperInitializer
import eu.simonbinder.kart.transformer.context.InBodyCompilationContext
import eu.simonbinder.kart.transformer.context.InClassContext
import eu.simonbinder.kart.transformer.context.names
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.util.file

object ClassMemberCompiler : BaseMemberCompiler<InClassContext>() {

    override val isStatic: Boolean
        get() = false

    override fun visitConstructor(declaration: IrConstructor, data: InClassContext) {
        // If the constructor calls another constructor (e.g. `constructor(): super()` or
        // via `: this()`, that would be represented in the IR body. But since those
        // things are supported in Dart directly, we try to reverse it.
        var body = declaration.body as? IrBlockBody
        val statements = body?.statements?.toMutableList()

        val initializer = (statements?.firstOrNull() as? IrDelegatingConstructorCall)?.let { call ->
            val isSuper = call.symbol.descriptor.constructedClass !=
                    declaration.symbol.descriptor.constructedClass

            val target = if (data.info.dartIntrinsics.isDefaultObjectConstructor(call)) {
                data.names.dartNames.objectDefaultConstructor.asReference()
            } else {
                data.names.nameFor(call.symbol.owner)
            }

            val (context, _) = createBodyContextAndParams(declaration, data)

            val arguments = Arguments(
                positional = List(call.valueArgumentsCount) { index ->
                    call.getValueArgument(index)!!.accept(ExpressionCompiler, context)
                }
            )

            if (isSuper) {
                SuperInitializer(target, arguments)
            }  else {
                RedirectingInitializer(target, arguments)
            }
        }

        if (initializer != null) {
            statements.removeAt(0)
            body = IrBlockBodyImpl(body!!.startOffset, body.endOffset, statements)
        }

        val dartConstructor = Constructor(
            reference = data.names.nameFor(declaration),
            name = Name(""),
            fileUri = data.info.loadFile(declaration.file),
            function = compileFunctionNode(declaration, data, body)
        )
        if (initializer != null) {
            dartConstructor.initializers.add(initializer)
        }

        data.target.members.add(dartConstructor)
    }
}