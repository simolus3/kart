package eu.simonbinder.kart.kernel.expressions

import eu.simonbinder.kart.kernel.TreeVisitor
import eu.simonbinder.kart.kernel.statements.VariableDeclaration
import eu.simonbinder.kart.kernel.types.DartType
import eu.simonbinder.kart.kernel.utils.child

class VariableGet(
    var variable: VariableDeclaration,
    var promotedType: DartType? = null
) : Expression() {

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitVariableGet(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {}
}

class VariableSet(
    var variable: VariableDeclaration,
    value: Expression? = null
) : Expression() {

    var value by child(value)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitVariableSet(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        value.accept(visitor)
    }

}