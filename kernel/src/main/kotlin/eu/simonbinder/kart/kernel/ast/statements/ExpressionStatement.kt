package eu.simonbinder.kart.kernel.ast.statements

import eu.simonbinder.kart.kernel.ast.TreeVisitor
import eu.simonbinder.kart.kernel.ast.expressions.Expression
import eu.simonbinder.kart.kernel.utils.child

class ExpressionStatement(expression: Expression): Statement() {

    val expression: Expression by child(expression)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitExpressionStatement(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        expression.accept(visitor)
    }
}