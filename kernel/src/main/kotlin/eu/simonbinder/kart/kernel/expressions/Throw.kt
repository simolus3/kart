package eu.simonbinder.kart.kernel.expressions

import eu.simonbinder.kart.kernel.TreeVisitor
import eu.simonbinder.kart.kernel.utils.child

class Throw(value: Expression? = null) : Expression() {

    var value by child(value)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitThrow(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        value.accept(visitor)
    }

}