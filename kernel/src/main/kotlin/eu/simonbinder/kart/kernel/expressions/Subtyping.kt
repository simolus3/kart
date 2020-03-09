package eu.simonbinder.kart.kernel.expressions

import eu.simonbinder.kart.kernel.TreeVisitor
import eu.simonbinder.kart.kernel.types.DartType
import eu.simonbinder.kart.kernel.utils.child

class IsExpression(
    operand: Expression,
    val targetType: DartType,
    val respectNullability: Boolean = true
) : Expression() {

    var operand: Expression by child(operand)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitIsExpression(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        operand.accept(visitor)
    }
}

class AsExpression(
    operand: Expression,
    val targetType: DartType,
    val isTypeError: Boolean = false,
    val isForDynamic: Boolean = false,
    val respectNullability: Boolean = true
) : Expression() {

    var operand: Expression by child(operand)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitAsExpression(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        operand.accept(visitor)
    }
}