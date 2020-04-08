package eu.simonbinder.kart.kernel.ast.statements

import eu.simonbinder.kart.kernel.ast.TreeVisitor
import eu.simonbinder.kart.kernel.ast.expressions.Expression
import eu.simonbinder.kart.kernel.utils.nullableChild

class ReturnStatement(expression: Expression? = null) : Statement() {

    var expression: Expression? by nullableChild(expression)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitReturn(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        expression?.accept(visitor)
    }
}