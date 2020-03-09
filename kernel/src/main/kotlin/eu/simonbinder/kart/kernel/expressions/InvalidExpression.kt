package eu.simonbinder.kart.kernel.expressions

import eu.simonbinder.kart.kernel.TreeVisitor

class InvalidExpression(val message: String) : Expression() {
    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitInvalidExpression(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {}
}