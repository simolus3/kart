package eu.simonbinder.kart.transformer.compilers

import eu.simonbinder.kart.kernel.ast.expressions.Expression
import eu.simonbinder.kart.kernel.ast.statements.*
import eu.simonbinder.kart.transformer.context.InBodyCompilationContext
import eu.simonbinder.kart.transformer.withIrOffsets
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

object BodyCompiler : IrElementVisitor<Statement, InBodyCompilationContext> {

    private fun compileExpression(expression: IrExpression, context: InBodyCompilationContext): Expression {
        return expression.accept(ExpressionCompiler, context)
    }

    override fun visitElement(element: IrElement, data: InBodyCompilationContext): Statement {
        throw NotImplementedError()
    }

    override fun visitExpression(expression: IrExpression, data: InBodyCompilationContext): Statement {
        return ExpressionStatement(compileExpression(expression, data))
    }

    override fun visitBlock(expression: IrBlock, data: InBodyCompilationContext): Statement {
        return Block(expression.statements.map { it.accept(this, data) })
    }

    override fun visitBody(body: IrBody, data: InBodyCompilationContext) = when (body) {
        is IrBlockBody -> Block(body.statements.map { it.accept(this, data) })
        is IrExpressionBody -> visitExpression(
            body.expression,
            data
        )
        else -> throw NotImplementedError("Body kind: $body")
    }

    private inline fun compileIrLoop(
        loop: IrLoop,
        data: InBodyCompilationContext,
        loopCreator: (condition: Expression, body: Statement) -> Statement
    ): Statement {
        val metadata = data.info.meta.metaForLoop(loop)

        var inner = loop.body?.accept(this, data) ?: EmptyStatement()
        // wrap loop body in labeled statement if its necessary (also see discussion at innerLabelForContinue)
        if (metadata.hasContinue) {
            inner = metadata.innerLabelForContinue.also {
                it.body = inner
            }
        }

        val dartLoop = loopCreator(compileExpression(loop.condition, data), inner).withIrOffsets(loop)
        return if (metadata.hasBreak) {
            metadata.outerLabelForBreak.also {
                it.body = dartLoop
            }
        } else {
            dartLoop
        }
    }

    override fun visitBreak(jump: IrBreak, data: InBodyCompilationContext): Statement {
        val meta = data.info.meta.metaForLoop(jump.loop)
        meta.hasBreak = true
        return BreakStatement(meta.outerLabelForBreak).withIrOffsets(jump)
    }

    override fun visitContinue(jump: IrContinue, data: InBodyCompilationContext): Statement {
        val meta = data.info.meta.metaForLoop(jump.loop)
        meta.hasContinue = true
        return BreakStatement(meta.innerLabelForContinue).withIrOffsets(jump)
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: InBodyCompilationContext): Statement {
        return compileIrLoop(loop, data, ::WhileStatement)
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: InBodyCompilationContext): Statement {
        return compileIrLoop(loop, data, ::DoStatement)
    }

    override fun visitReturn(expression: IrReturn, data: InBodyCompilationContext): Statement {
        // todo: Can we be sure that each return only refers to the innermost function here?
        return ReturnStatement(compileExpression(expression.value, data)).withIrOffsets(expression)
    }

    override fun visitTry(aTry: IrTry, data: InBodyCompilationContext): Statement {
        var stmt: Statement = TryCatch(
            body = aTry.tryResult.accept(this, data),
            catches = aTry.catches.map { irCatch ->
                val dartType = data.info.dartTypeFor(irCatch.catchParameter.type)
                Catch(
                    guard = dartType,
                    body = irCatch.result.accept(this, data),
                    exception = VariableDeclaration(
                        name = irCatch.catchParameter.name.identifier,
                        type = dartType
                    ).also { it.isFinal = true }
                )
            }
        )

        aTry.finallyExpression?.let { irFinally ->
            stmt = TryFinally(stmt, irFinally.accept(this, data))
        }

        return stmt
    }

    override fun visitVariable(declaration: IrVariable, data: InBodyCompilationContext): Statement {
        val kernelDeclaration = VariableDeclaration(
            name = declaration.name.identifier,
            type = data.info.dartTypeFor(declaration.type),
            initializer = declaration.initializer?.let {
                compileExpression(it, data)
            }
        )
        data.variables[declaration.symbol] = kernelDeclaration

        kernelDeclaration.isFinal = !declaration.isVar
        kernelDeclaration.isInScope = true
        kernelDeclaration.isLate = declaration.isLateinit

        kernelDeclaration.fileOffset = declaration.startOffset

        return kernelDeclaration
    }

    override fun visitWhen(expression: IrWhen, data: InBodyCompilationContext): Statement {
        var root: IfStatement? = null
        lateinit var currentBranch: IfStatement

        for (branch in expression.branches) {
            val dartCondition = compileExpression(branch.condition, data)
            val then = branch.result.accept(this, data)
            val dartIf = IfStatement(dartCondition, then).withIrOffsets(branch)

            when {
                root == null -> {
                    root = dartIf
                    currentBranch = dartIf
                }
                branch is IrElseBranch -> {
                    currentBranch.otherwise = then
                }
                else -> {
                    currentBranch.otherwise = dartIf
                    currentBranch = dartIf
                }
            }
        }

        return root!!
    }
}