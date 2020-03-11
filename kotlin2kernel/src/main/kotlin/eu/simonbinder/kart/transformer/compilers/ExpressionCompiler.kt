package eu.simonbinder.kart.transformer.compilers

import eu.simonbinder.kart.kernel.Reference
import eu.simonbinder.kart.kernel.expressions.*
import eu.simonbinder.kart.transformer.context.InBodyCompilationContext
import eu.simonbinder.kart.transformer.context.names
import eu.simonbinder.kart.transformer.withIrOffsets
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeOp
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.utils.addToStdlib.cast
import kotlin.math.exp

object ExpressionCompiler : IrElementVisitor<Expression, InBodyCompilationContext> {

    override fun visitElement(element: IrElement, data: InBodyCompilationContext): Expression {
        throw NotImplementedError("Can't compile expression: $element")
    }

    override fun visitBlock(expression: IrBlock, data: InBodyCompilationContext): Expression {
        val bodyWithoutReturn = expression.statements
            .asSequence()
            .map { it.accept(BodyCompiler, data) }
            .take(expression.statements.size - 1)
            .toList()

        val irReturn = expression.statements.last()
        if (irReturn !is IrExpression) {
            throw IllegalArgumentException("Can't compile IrBlock as expression if last stmt isn't an expression")
        }

        return BlockExpression(body = bodyWithoutReturn, value = irReturn.accept(this, data))
    }

    override fun <T> visitConst(expression: IrConst<T>, data: InBodyCompilationContext) = when (expression.kind) {
        IrConstKind.Null -> NullLiteral()
        IrConstKind.Boolean -> BooleanLiteral(expression.value.cast())
        IrConstKind.String -> StringLiteral(expression.value.cast())
        IrConstKind.Long -> IntegerLiteral(expression.value.cast())
        IrConstKind.Double -> DoubleLiteral(expression.value.cast())
        else -> throw NotImplementedError("Constant should have been desugared to Long or Double: $expression")
    }

    override fun visitCall(expression: IrCall, data: InBodyCompilationContext): Expression {
        var intrinsic = data.info.dartIntrinsics.intrinsicCall(expression) {
            it.accept(this, data)
        }
        if (intrinsic != null) return intrinsic.withIrOffsets(expression)

        // temporarily implement some more calls as intrinsics
        intrinsic = when (expression.symbol.descriptor.name.identifier) {
            "println" -> {
                val args = Arguments(
                    positional = List(expression.valueArgumentsCount) { index ->
                        expression.getValueArgument(index)!!.accept(this, data)
                    }
                )

                val printName = data.names.root.getChild("dart:core").getChild("@methods").getChild("print")
                StaticInvocation(Reference(printName), args)
            }
            else -> null
        }
        if (intrinsic != null) return intrinsic.withIrOffsets(expression)

        val dartFunctionReference = data.names.nameFor(expression.symbol.owner)
        return StaticInvocation(dartFunctionReference, Arguments(
            positional = List(expression.valueArgumentsCount) { index ->
                expression.getValueArgument(index)!!.accept(this, data)
            }
        )).withIrOffsets(expression)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: InBodyCompilationContext): Expression {
        val compiledArg = expression.argument.accept(this, data)
        val type = data.info.dartTypeFor(expression.typeOperand)

        return when (val operator = expression.operator) {
            IrTypeOperator.INSTANCEOF -> IsExpression(compiledArg, type).also {
                it.withIrOffsets(expression)
                it.isForNonNullableByDefault = true
            }
            IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST -> AsExpression(compiledArg, type).also {
                it.withIrOffsets(expression)
                // The Dart frontend sets the isTypeError flag on an implicit cast, so we do the same
                it.isTypeError = operator == IrTypeOperator.IMPLICIT_CAST
                it.isForNonNullableByDefault = true
            }
            else -> TODO()
        }
    }

    override fun visitErrorExpression(expression: IrErrorExpression, data: InBodyCompilationContext): Expression {
        return InvalidExpression(expression.description).withIrOffsets(expression)
    }

    override fun visitGetValue(expression: IrGetValue, data: InBodyCompilationContext): Expression {
        val kernelDeclaration = data.variables[expression.symbol]
            ?: throw IllegalStateException("GetValue $expression referred to an unknown variable")

        return VariableGet(kernelDeclaration).withIrOffsets(expression)
    }

    override fun visitSetVariable(expression: IrSetVariable, data: InBodyCompilationContext): Expression {
        val kernelDeclaration = data.variables[expression.symbol]
            ?: throw IllegalStateException("SetVariable $expression referred to an unknown variable")

        return VariableSet(kernelDeclaration, expression.value.accept(this, data)).withIrOffsets(expression)
    }

    override fun visitStringConcatenation(
        expression: IrStringConcatenation,
        data: InBodyCompilationContext
    ): Expression {
        return StringConcatenation(expression.arguments.map { it.accept(this, data) }).withIrOffsets(expression)
    }

    override fun visitThrow(expression: IrThrow, data: InBodyCompilationContext): Expression {
        return Throw(expression.value.accept(this, data)).withIrOffsets(expression)
    }

    override fun visitWhen(expression: IrWhen, data: InBodyCompilationContext): Expression {
        var root: ConditionalExpression? = null
        lateinit var current: ConditionalExpression

        // create a chain of ternary expressions
        for (branch in expression.branches) {
            val dartBranch = branch.condition.accept(this, data)
            val dartResult = branch.result.accept(this, data)
            val dartCondition = ConditionalExpression(dartBranch, dartResult, InvalidExpression("No else set"))

            when {
                root == null -> {
                    root = dartCondition
                    current = dartCondition
                }
                branch is IrElseBranch -> {
                    current.otherwise = dartResult
                }
                else -> {
                    current.otherwise = dartCondition
                    current = dartCondition
                }
            }
        }

        return root!!
    }
}