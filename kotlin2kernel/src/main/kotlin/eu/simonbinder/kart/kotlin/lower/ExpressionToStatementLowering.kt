package eu.simonbinder.kart.kotlin.lower

import eu.simonbinder.kart.kotlin.DartBackendContext
import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.Name

class ExpressionToStatementLowering(private val context: DartBackendContext) : FunctionLoweringPass {
    override fun lower(irFunction: IrFunction) {
        val body = irFunction.body ?: return
        val builder = context.createIrBuilder(irFunction.symbol).at(body)

        val transformer = ExpressionToSetVariableTransformer(builder)
        irFunction.body = body.transform(transformer, ExpressionUsage.STATEMENT)
    }
}

/**
 * Turns Kotlin IR expressions that aren't an expression in Kernel into statements by
 * introducing local variables.
 *
 * For instance, let's say we had the following Kotlin code:
 * ```kotlin
 * fun foo() {
 *   val x = try {
 *     bar() ?: return
 *   } catch (e: Throwable) {
 *     baz();
 *   }
 * }
 * ```
 *
 * Here, [IrTry] is used as an expression that can't be represented in Dart. We can solve
 * this by introducing a local variable:
 * ```kotlin
 * fun foo() {
 *   val x = {
 *     lateinit var temp0: TypeOfX
 *     try {
 *       temp0 = bar() ?: return
 *     } catch (e: Throwable) {
 *       temp0 = baz();
 *     }
 *     temp_0
 *   }
 * }
 * ```
 *
 * The `temp0 = bar() ?: return` would be transformed in a similar way.
 */
private class ExpressionToSetVariableTransformer(
    val builder: IrBuilderWithScope
) : IrElementTransformer<ExpressionUsage> {

    var tmpVarCount = 0

    // When data != null, we're supposed to make the expression write its value into that
    // temporary variable we allocate.

    override fun visitBlock(expression: IrBlock, data: ExpressionUsage) = expression.also {
        for ((i, statement) in it.statements.withIndex()) {
            it.statements[i] = if (i == it.statements.size - 1) {
                // only the last statement in a block is used as an expression that might need
                // transformations
                statement.transform(this, data)
            } else {
                // However, others might need an independent transformation, so we still visit them
                statement.transform(this, ExpressionUsage.STATEMENT)
            }
        }
    }

    override fun visitBranch(branch: IrBranch, data: ExpressionUsage): IrBranch = branch.also {
        it.condition = it.condition.transform(this, ExpressionUsage.INDEPENDENT_EXPRESSION)
        it.result = it.result.transform(this, data)
    }

    override fun visitBreakContinue(jump: IrBreakContinue, data: ExpressionUsage): IrExpression {
        return jump.transformToVariableSet(data, true) {
            jump
        }
    }

    override fun visitExpression(expression: IrExpression, data: ExpressionUsage): IrExpression {
        return if (!data.valueIsUsed || data.currentExpandingVariable == null) {
            expression.transformChildren(this, data)
            expression
        } else {
            expression.transformChildren(this, ExpressionUsage.INDEPENDENT_EXPRESSION)
            builder.at(expression).irSetVar(data.currentExpandingVariable.symbol, expression)
        }
    }

    override fun visitLoop(loop: IrLoop, data: ExpressionUsage): IrExpression {
        // When loops are evaluated as expressions, it refers to their body
        loop.body = loop.body?.transform(this, data)

        // Their condition is independent, but might require lowering as well
        // (if people start using try blocks as loop condition or something)
        loop.condition = loop.condition.transform(this, ExpressionUsage.INDEPENDENT_EXPRESSION)

        return loop
    }

    override fun visitSetVariable(expression: IrSetVariable, data: ExpressionUsage) = expression.also {
        expression.value = expression.value.transform(this, ExpressionUsage.INDEPENDENT_EXPRESSION)
    }

    override fun visitSetField(expression: IrSetField, data: ExpressionUsage) = expression.also {
        expression.value = expression.value.transform(this, ExpressionUsage.INDEPENDENT_EXPRESSION)
    }

    override fun visitTry(aTry: IrTry, data: ExpressionUsage): IrExpression {
        return aTry.transformToVariableSet(data) { usage ->
            aTry.transformChildren(this@ExpressionToSetVariableTransformer, usage)
            aTry
        }
    }

    override fun visitCatch(aCatch: IrCatch, data: ExpressionUsage): IrCatch {
        // only transform the result, not the parameter
        aCatch.result = aCatch.result.transform(this, data)
        return aCatch
    }

    override fun visitReturn(expression: IrReturn, data: ExpressionUsage) = expression.also {
        it.value = it.value.transform(this, ExpressionUsage.INDEPENDENT_EXPRESSION)
    }

    override fun visitVariable(declaration: IrVariable, data: ExpressionUsage) = declaration.also {
        it.initializer = it.initializer?.transform(this, ExpressionUsage.INDEPENDENT_EXPRESSION)
    }

    override fun visitWhen(expression: IrWhen, data: ExpressionUsage): IrExpression {
        // Note: We're turning a when with more than two branches into an if because deeply nested
        // conditional expressions are annoying to read. We should probably reverse this when the
        // compiler is more stable.
        return if (expression.branches.size >= 3) {
            expression.transformToVariableSet(data) { usage ->
                expression.transformChildren(this@ExpressionToSetVariableTransformer, usage)
                expression
            }
        } else {
            super.visitWhen(expression, data)
        }
    }

    private fun IrExpression.transformToVariableSet(
        data: ExpressionUsage,
        neverEvaluates: Boolean = false,
        body: (usage: ExpressionUsage) -> IrExpression
    ): IrExpression {
        if (!data.valueIsUsed) {
            // regular statement, nothing to apply here
            transformChildren(this@ExpressionToSetVariableTransformer, data)
            return this
        }

        if (data.currentExpandingVariable != null && neverEvaluates) {
            // Let's say we have something like `val x = try { foo() } catch (e: Throwable) { break }` and we're
            // currently transforming the `break` expression. Since we already have a variable that was created while
            // transforming the surrounding try-catch, we don't need to write anything to that variable or create
            // another body. So instead of
            // val x = {
            //  lateinit var temp_0: TypeOfX
            //  try {
            //    foo()
            //  } catch (e: Throwable) {
            //    {
            //      break
            //      temp_0
            //    }
            //  }
            // We just use a single break in the catch clause.
            return body(data)
        }

        return builder.at(this).irBlock {
            var didIntroduceVariable = false
            val childrenUsage = if (data.currentExpandingVariable != null) {
                data
            } else {
                val variable = createTemporaryVariable(type)
                +variable
                didIntroduceVariable = true
                ExpressionUsage(true, variable)
            }

            +body(childrenUsage)

            if (didIntroduceVariable || !neverEvaluates) {
                +irGet(childrenUsage.currentExpandingVariable!!)
            }
        }
    }

    private fun createTemporaryVariable(type: IrType): IrVariable {
        val name = "tmp_${tmpVarCount++}"
        val descriptor = WrappedVariableDescriptor()
        val symbol = IrVariableSymbolImpl(descriptor)

        return IrVariableImpl(
            -1, // start and end offset
            -1,
            IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
            symbol,
            Name.identifier(name),
            type,
            true, // mutable
            isConst = false,
            isLateinit = true
        ).also {
            descriptor.bind(it)
        }
    }

}

private class ExpressionUsage(
    val valueIsUsed: Boolean,
    val currentExpandingVariable: IrVariable? = null
) {
    companion object {
        val STATEMENT = ExpressionUsage(false)
        val INDEPENDENT_EXPRESSION = ExpressionUsage(true)
    }

}