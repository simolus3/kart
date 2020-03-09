package eu.simonbinder.kart.kernel.expressions

import eu.simonbinder.kart.kernel.TreeVisitor
import eu.simonbinder.kart.kernel.utils.child

/**
 * A null check is similar to the `!!` operator in Kotlin.
 */
class NullCheck(operand: Expression) : Expression() {

    var operand: Expression by child(operand)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitNullCheck(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        operand.visitChildren(visitor)
    }

}