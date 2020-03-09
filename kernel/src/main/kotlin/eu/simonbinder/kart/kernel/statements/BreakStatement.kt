package eu.simonbinder.kart.kernel.statements

import eu.simonbinder.kart.kernel.TreeVisitor

class BreakStatement(
    var to: LabeledStatement? = null
): Statement() {

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitBreak(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        // note that to is not a child of this statement, we just refer to it
    }
}