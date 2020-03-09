package eu.simonbinder.kart.kernel.statements

import eu.simonbinder.kart.kernel.TreeVisitor
import eu.simonbinder.kart.kernel.expressions.Expression
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