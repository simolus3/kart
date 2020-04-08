package eu.simonbinder.kart.kernel.ast.statements

import eu.simonbinder.kart.kernel.ast.TreeVisitor

class EmptyStatement : Statement() {
    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitEmptyStatement(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {}
}