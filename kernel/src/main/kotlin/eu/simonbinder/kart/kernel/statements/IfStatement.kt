package eu.simonbinder.kart.kernel.statements

import eu.simonbinder.kart.kernel.TreeVisitor
import eu.simonbinder.kart.kernel.expressions.Expression
import eu.simonbinder.kart.kernel.utils.child

class IfStatement(
    condition: Expression,
    then: Statement,
    otherwise: Statement = EmptyStatement()
): Statement() {

    var condition: Expression by child(condition)
    var then: Statement by child(then)
    var otherwise: Statement by child(otherwise)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitIfStatement(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        condition.accept(visitor)
        then.accept(visitor)
        otherwise.accept(visitor)
    }
}