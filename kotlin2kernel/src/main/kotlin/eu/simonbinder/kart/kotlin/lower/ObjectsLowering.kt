package eu.simonbinder.kart.kotlin.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

// We're not using the lowering from the common Kotlin compiler because it seems to only target js.
// And, instead of generating a field + getter, this one only generates a field.

class ObjectDeclarationLowering(val context: CommonBackendContext) : DeclarationContainerLoweringPass {

    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.transformDeclarationsFlat { declaration ->
            if (declaration !is IrClass || !declaration.isObject || declaration.isEffectivelyExternal()) {
                return@transformDeclarationsFlat listOf(declaration)
            }

            val irClass: IrClass = declaration
            val constructor = irClass.primaryConstructor ?: error("Object class didn't have a primary constructor")
            val field = createInstanceField(irClass, constructor)

            // Objects can use their instance in the constructor and elsewhere, which might be before the field was
            // initialized. We just transform those to `this`.
            val thisInstance = irClass.thisReceiver ?: return@transformDeclarationsFlat listOf(declaration)

            irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
                    return if (expression.symbol == irClass.symbol) {
                        IrGetValueImpl(-1, -1, thisInstance.symbol, IrStatementOrigin.GET_PROPERTY)
                    } else {
                        expression
                    }
                }
            })

            listOf(field, declaration)
        }
    }

    private fun createInstanceField(irObject: IrClass, constructor: IrConstructor): IrField {
        val field = context.declarationFactory.getFieldForObjectInstance(irObject)
        val builder = context.createIrBuilder(field.symbol)

        val initializer = builder.irCallConstructor(constructor.symbol, typeArguments = emptyList())
        field.initializer = builder.irExprBody(initializer)

        return field
    }
}

class ObjectUsageLowering(val context: CommonBackendContext): BodyLoweringPass {
    override fun lower(irBody: IrBody) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
                val obj = expression.symbol.owner
                if (obj.isEffectivelyExternal()) return expression

                val instanceField = context.declarationFactory.getFieldForObjectInstance(obj)
                return IrGetFieldImpl(
                    expression.startOffset,
                    expression.endOffset,
                    instanceField.symbol,
                    expression.type
                )
            }
        })
    }

}