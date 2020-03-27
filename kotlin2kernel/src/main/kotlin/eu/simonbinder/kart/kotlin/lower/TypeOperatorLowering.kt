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

object NotInstanceOfOrigin : IrStatementOriginImpl("NOT_INSTANCEOF")

class TypeOperatorLowering(val context: DartBackendContext): FunctionLoweringPass {
    override fun lower(irFunction: IrFunction) {
        val builder = context.createIrBuilder(irFunction.symbol)

        irFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                return when (expression.operator) {
                    // Nothing is necessary for implicit integer coercions, since all Kotlin integer types map to
                    // dart.core::int. (todo: Do we need to and an appropriate bitmask when going to smaller types?)
                    // kotlin.Unit is represented as VoidType in Kernel, which is a top type. Since any value can be
                    // assigned to it, coercing to unit is a no-op as well.
                    IrTypeOperator.IMPLICIT_INTEGER_COERCION, IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                        expression.argument
                    }
                    IrTypeOperator.NOT_INSTANCEOF -> {
                        // NOT_INSTANCEOF => !INSTANCEOF

                        primitiveOp1(
                            expression.startOffset, expression.endOffset,
                            context.irBuiltIns.booleanNotSymbol,
                            context.irBuiltIns.booleanType,
                            NotInstanceOfOrigin,
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
                }.also { it.transformChildrenVoid(this) }
            }
        })
    }

}