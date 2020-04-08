package eu.simonbinder.kart.kernel.ast.expressions

import eu.simonbinder.kart.kernel.ast.TreeVisitor
import eu.simonbinder.kart.kernel.ast.statements.Statement
import eu.simonbinder.kart.kernel.utils.child
import eu.simonbinder.kart.kernel.utils.children

class BlockExpression(
    body: List<Statement>? = null,
    value: Expression? = null
) : Expression() {

    val body: List<Statement> = children(body)
    var value: Expression by child(value)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitBlockExpression(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        body.forEach { it.accept(visitor) }
        value.accept(visitor)
    }

}