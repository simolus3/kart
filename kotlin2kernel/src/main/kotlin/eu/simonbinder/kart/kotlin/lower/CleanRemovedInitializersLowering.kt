package eu.simonbinder.kart.kotlin.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.backend.common.lower.InitializersLowering
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Removes initializers that were made obsolete by [InitializersLowering].
 */
class CleanRemovedInitializersLowering : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitField(declaration: IrField): IrStatement {
                if (!declaration.isStatic) {
                    declaration.initializer = null
                }

                return declaration
            }
        })
    }
}