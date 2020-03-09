package eu.simonbinder.kart.kernel.statements

import eu.simonbinder.kart.kernel.TreeVisitor
import eu.simonbinder.kart.kernel.expressions.Expression
import eu.simonbinder.kart.kernel.utils.child

class WhileStatement(
    condition: Expression? = null,
    body: Statement? = null
) : Statement() {

    var condition by child(condition)
    var body by child(body)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitWhile(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        condition.accept(visitor)
        body.accept(visitor)
    }
}