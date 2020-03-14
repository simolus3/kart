package eu.simonbinder.kart.transformer.compilers

import eu.simonbinder.kart.transformer.context.InLibraryContext
import eu.simonbinder.kart.transformer.identifierOrNull
import org.jetbrains.kotlin.ir.declarations.IrFunction

object LibraryCompiler : BaseMemberCompiler<InLibraryContext>() {

    override val isStatic: Boolean
        get() = true

    override fun visitFunction(declaration: IrFunction, data: InLibraryContext) {
        val procedure = compileFunction(declaration, data)
        data.target.members.add(procedure)

        if (declaration.isMain()) {
            data.info.component.mainMethod = procedure
        }
    }

    private fun IrFunction.isMain(): Boolean {
        return name.identifierOrNull == "main" &&
                dispatchReceiverParameter == null &&
                extensionReceiverParameter == null &&
                valueParameters.isEmpty()
    }
}