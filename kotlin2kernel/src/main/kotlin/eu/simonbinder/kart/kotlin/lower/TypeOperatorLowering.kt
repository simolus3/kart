package eu.simonbinder.kart.kotlin.lower

import eu.simonbinder.kart.kotlin.DartBackendContext
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class TypeOperatorLowering(val context: DartBackendContext): FunctionLoweringPass {
    override fun lower(irFunction: IrFunction) {
        val builder = context.createIrBuilder(irFunction.symbol)

        irFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                return when (expression.operator) {
                    // Byte, Short, etc. are all dart.core::int, so nothing is necessary here
                    IrTypeOperator.IMPLICIT_INTEGER_COERCION -> expression.argument
                    IrTypeOperator.NOT_INSTANCEOF -> {
                        // NOT_INSTANCEOF => !INSTANCEOF

                        primitiveOp1(
                            expression.startOffset, expression.endOffset,
                            context.irBuiltIns.booleanNotSymbol,
                            context.irBuiltIns.booleanType,
                            IrStatementOrigin.NOT_IN, // todo: What's the right origin here? Certainly not NOT_IN
                            IrTypeOperatorCallImpl(
                                expression.startOffset,
                                expression.endOffset,
                                expression.type,
                                IrTypeOperator.INSTANCEOF,
                                expression.typeOperand,
                                expression.argument
                            )
                        )
                    }
                    IrTypeOperator.SAFE_CAST -> {
                        // x as? T => if (x is T) x else null
                        builder.at(expression).irComposite {
                            val tempVar = irTemporary(expression.argument)
                            +tempVar
                            +irWhen(
                                type = expression.type,
                                branches = listOf(
                                    irBranch(
                                        condition = irIs(irGet(tempVar), expression.typeOperand),
                                        result = irGet(tempVar)
                                    ),
                                    irElseBranch(irNull())
                                )
                            )
                        }
                     }
                    else -> expression
                }
            }
        })
    }

}