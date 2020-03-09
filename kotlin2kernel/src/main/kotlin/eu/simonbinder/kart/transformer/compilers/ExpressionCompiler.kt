package eu.simonbinder.kart.transformer.compilers

import eu.simonbinder.kart.kernel.Reference
import eu.simonbinder.kart.kernel.expressions.*
import eu.simonbinder.kart.transformer.context.InBodyCompilationContext
import eu.simonbinder.kart.transformer.context.names
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.utils.addToStdlib.cast

object ExpressionCompiler : IrElementVisitor<Expression, InBodyCompilationContext> {

    override fun visitElement(element: IrElement, data: InBodyCompilationContext): Expression {
        // todo: We should probably apply some transformation so that these don't appear as expressions
        if (element is IrBreakContinue || element is IrReturn) {
            return BlockExpression(
                body = listOf(element.accept(BodyCompiler, data)),
                value = InvalidExpression("Should not get here")
            )
        }

        throw NotImplementedError("Can't compile expression: $element")
    }

    override fun visitBlock(expression: IrBlock, data: InBodyCompilationContext): Expression {
        expression.getPackageFragment()
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
        if (intrinsic != null) return intrinsic

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
        if (intrinsic != null) return intrinsic

        val dartFunctionReference = data.names.nameFor(expression.symbol.owner)
        return StaticInvocation(dartFunctionReference, Arguments(
            positional = List(expression.valueArgumentsCount) { index ->
                expression.getValueArgument(index)!!.accept(this, data)
            }
        ))
    }

    override fun visitErrorExpression(expression: IrErrorExpression, data: InBodyCompilationContext): Expression {
        return InvalidExpression(expression.description)
    }

    override fun visitGetValue(expression: IrGetValue, data: InBodyCompilationContext): Expression {
        val kernelDeclaration = data.variables[expression.symbol]
            ?: throw IllegalStateException("GetValue $expression referred to an unknown variable")

        return VariableGet(kernelDeclaration)
    }

    override fun visitSetVariable(expression: IrSetVariable, data: InBodyCompilationContext): Expression {
        val kernelDeclaration = data.variables[expression.symbol]
            ?: throw IllegalStateException("SetVariable $expression referred to an unknown variable")

        return VariableSet(kernelDeclaration, expression.value.accept(this, data))
    }

    override fun visitStringConcatenation(
        expression: IrStringConcatenation,
        data: InBodyCompilationContext
    ): Expression = StringConcatenation(expression.arguments.map { it.accept(this, data) })

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