package eu.simonbinder.kart.kernel.ast.statements

import eu.simonbinder.kart.kernel.ast.TreeVisitor
import eu.simonbinder.kart.kernel.ast.expressions.Expression
import eu.simonbinder.kart.kernel.utils.child
import eu.simonbinder.kart.kernel.utils.nullableChild

class AssertStatement(condition: Expression, message: Expression?): Statement() {

    var condition: Expression by child(condition)
    var message: Expression? by nullableChild(message)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitAssertStatement(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        condition.accept(visitor)
        message?.accept(visitor)
    }
}