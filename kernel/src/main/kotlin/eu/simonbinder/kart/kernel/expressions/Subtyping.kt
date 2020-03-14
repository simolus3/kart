package eu.simonbinder.kart.kernel.expressions

import eu.simonbinder.kart.kernel.TreeVisitor
import eu.simonbinder.kart.kernel.types.DartType
import eu.simonbinder.kart.kernel.utils.HasFlags
import eu.simonbinder.kart.kernel.utils.child
import eu.simonbinder.kart.kernel.utils.flag

class IsExpression(
    operand: Expression,
    val targetType: DartType
) : Expression(), HasFlags {

    override var flags: Int = 0
    var operand: Expression by child(operand)

    var isForNonNullableByDefault by flag(0)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitIsExpression(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        operand.accept(visitor)
    }
}

class AsExpression(
    operand: Expression,
    val targetType: DartType
) : Expression(), HasFlags {

    var operand: Expression by child(operand)
    override var flags: Int = 0

    var isTypeError by flag(0)
    var isCovarianceCheck by flag(1)
    var isForDynamic by flag(2)
    var isForNonNullableByDefault by flag(3)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitAsExpression(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        operand.accept(visitor)
    }
}