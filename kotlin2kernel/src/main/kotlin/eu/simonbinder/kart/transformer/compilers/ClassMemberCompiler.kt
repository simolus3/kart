package eu.simonbinder.kart.transformer.compilers

import eu.simonbinder.kart.transformer.context.InClassContext
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction

object ClassMemberCompiler : BaseMemberCompiler<InClassContext>() {

    override val isStatic: Boolean
        get() = false

    override fun visitConstructor(declaration: IrConstructor, data: InClassContext) {

    }

    override fun visitFunction(declaration: IrFunction, data: InClassContext) {
        if (declaration.body != null) {
            super.visitFunction(declaration, data)
        }
    }
}