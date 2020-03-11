package eu.simonbinder.kart.kotlin.lower

import eu.simonbinder.kart.kotlin.DartBackendContext
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Transforms usages of `Byte`, `Char`, `Short` and `Int` into usages of `Long`. Usages of `Float` are transformed to
 * usages of `Double`.
 */
class PrimitiveTypeLowering(private val context: DartBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody) {
        return irBody.transformChildrenVoid(PrimitiveLoweringTransformer(context))
    }
}

private class PrimitiveLoweringTransformer(private val context: DartBackendContext) : IrElementTransformerVoid() {

    private val longType get() = context.irBuiltIns.longType
    private val doubleType get() = context.irBuiltIns.doubleType

    override fun <T> visitConst(expression: IrConst<T>): IrExpression {
        return when (expression.kind) {
            is IrConstKind.Byte -> createLongConstant(expression, (expression.value as Byte).toLong())
            is IrConstKind.Char -> createLongConstant(expression, (expression.value as Char).toLong())
            is IrConstKind.Short -> createLongConstant(expression, (expression.value as Short).toLong())
            is IrConstKind.Int -> createLongConstant(expression, (expression.value as Int).toLong())
            is IrConstKind.Float -> createDoubleConstant(expression, (expression.value as Float).toDouble())
            else -> expression
        }
    }

    private fun createLongConstant(old: IrConst<*>, value: Long): IrExpression {
        return IrConstImpl.long(old.startOffset, old.endOffset, longType, value)
    }

    private fun createDoubleConstant(old: IrConst<*>, value: Double): IrExpression {
        return IrConstImpl.double(old.startOffset, old.endOffset, doubleType, value)
    }
}