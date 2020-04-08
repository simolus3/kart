package eu.simonbinder.kart.kernel.ast.expressions

import eu.simonbinder.kart.kernel.ast.TreeVisitor
import eu.simonbinder.kart.kernel.utils.child

class Not(operand: Expression ? = null) : Expression() {

    var operand: Expression by child(operand)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitNot(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        operand.accept(visitor)
    }

}

enum class LogicalOperator {
    AND,
    OR
}

/**
 * A short-circuit and/or expression with boolean operands.
 */
class LogicalExpression(
    var operator: LogicalOperator,
    left: Expression? = null,
    right: Expression? = null
) : Expression() {

    var left: Expression by child(left)
    var right: Expression by child(right)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitLogicalExpression(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        left.accept(visitor)
        right.accept(visitor)
    }

}