package eu.simonbinder.kart.kernel.expressions

import eu.simonbinder.kart.kernel.TreeVisitor

object This : Expression() {
    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitThis(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {}
}