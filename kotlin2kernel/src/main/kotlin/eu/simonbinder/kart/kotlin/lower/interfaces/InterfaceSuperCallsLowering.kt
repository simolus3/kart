package eu.simonbinder.kart.kotlin.lower.interfaces

import eu.simonbinder.kart.kotlin.DartBackendContext
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class InterfaceSuperCallsLowering(private val context: DartBackendContext): IrElementTransformerVoid(), BodyLoweringPass {
    override fun lower(irBody: IrBody) {
        irBody.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.superQualifierSymbol?.owner?.isInterface != true) {
            return super.visitCall(expression)
        }

        val superCallee = (expression.symbol.owner as IrSimpleFunction).resolveFakeOverride()!!
        val defaultImpl = context.declarationFactory.staticDefaultImplForInterface(superCallee)

        val callToDefault = irCall(expression, defaultImpl, receiversAsArguments = true)
        return super.visitCall(callToDefault)
    }

}