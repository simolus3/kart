package eu.simonbinder.kart.kotlin

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl

interface DartDeclarationOrigin : IrDeclarationOrigin {
    object LOWERED_INIT_BLOCK: IrDeclarationOriginImpl("LOWERED_INIT_BLOCK")
}

class DartFunctionOrigin(
    val name: String,
    override val isSynthetic: Boolean,
    val isStatic: Boolean
) : IrDeclarationOrigin {

    override fun toString() = "DartFunctionOrigin($name)"

    companion object {
        val StaticDefaultMethod = DartFunctionOrigin("StaticDefaultMethod", isSynthetic = true, isStatic = true)
    }
}

fun IrDeclarationOrigin.isStaticInDart() = this is DartFunctionOrigin && isStatic