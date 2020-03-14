package eu.simonbinder.kart.kernel.expressions

import eu.simonbinder.kart.kernel.Reference
import eu.simonbinder.kart.kernel.TreeVisitor
import eu.simonbinder.kart.kernel.utils.child

class StaticGet(
    val reference: Reference
) : Expression() {

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitStaticGet(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {}
}

class StaticSet(
    val reference: Reference,
    value: Expression? = null
) : Expression() {

    var value by child(value)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitStaticSet(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        value.accept(visitor)
    }
}