package eu.simonbinder.kart.kernel.expressions

import eu.simonbinder.kart.kernel.TreeVisitor
import eu.simonbinder.kart.kernel.types.DartType
import eu.simonbinder.kart.kernel.utils.child
import eu.simonbinder.kart.kernel.utils.nullableChild

class ConditionalExpression(
    condition: Expression,
    then: Expression,
    otherwise: Expression,
    var staticType: DartType? = null
) : Expression() {

    var condition: Expression by child(condition)
    var then: Expression by child(then)
    var otherwise: Expression by child(otherwise)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitConditionalExpression(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        condition.accept(visitor)
        then.accept(visitor)
        otherwise.accept(visitor)
    }
}