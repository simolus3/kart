package eu.simonbinder.kart.kotlin.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class ExtensionDeclarationLowering : FunctionLoweringPass {

    override fun lower(irFunction: IrFunction) {
        if (irFunction.isEffectivelyExternal()) return

        val receiverParameter = irFunction.extensionReceiverParameter ?: return
        irFunction.extensionReceiverParameter = null

        val newDescriptor = WrappedValueParameterDescriptor()
        val symbol = IrValueParameterSymbolImpl(newDescriptor)
        val newParam = IrValueParameterImpl(
            receiverParameter.startOffset,
            receiverParameter.endOffset,
            receiverParameter.origin,
            symbol,
            Name.identifier("\$this"),
            -1, // index
            receiverParameter.type,
            receiverParameter.varargElementType,
            isCrossinline = false,
            isNoinline = false
        )
        newDescriptor.bind(newParam)

        irFunction.valueParameters.add(0, newParam)
        irFunction.body?.transformChildrenVoid(ReceiverParameterReplacer(receiverParameter, newParam))
    }

    private class ReceiverParameterReplacer(
        val receiverParam: IrValueParameter,
        val newParam: IrValueParameter
    ) : IrElementTransformerVoid() {
        override fun visitGetValue(expression: IrGetValue): IrExpression {
            if (expression.symbol == receiverParam.symbol) {
                return IrGetValueImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    newParam.symbol,
                    expression.origin
                )
            }
            return expression
        }
    }
}

class ExtensionCallSiteLowering : BodyLoweringPass {
    override fun lower(irBody: IrBody) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val receiver = expression.extensionReceiver ?: return expression

                expression.extensionReceiver = null

                return IrCallImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    expression.symbol,
                    expression.descriptor,
                    expression.typeArgumentsCount,
                    expression.valueArgumentsCount + 1,
                    expression.origin,
                    expression.superQualifierSymbol
                ).also {
                    it.dispatchReceiver = expression.dispatchReceiver

                    it.putValueArgument(0, receiver)
                    repeat(expression.valueArgumentsCount) { index ->
                        it.putValueArgument(index + 1, expression.getValueArgument(index))
                    }

                    repeat(expression.typeArgumentsCount) { index ->
                        it.putTypeArgument(index, expression.getTypeArgument(index))
                    }
                }
            }
        })
    }

}
