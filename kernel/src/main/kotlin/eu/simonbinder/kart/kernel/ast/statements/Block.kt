package eu.simonbinder.kart.kernel.ast.statements

import eu.simonbinder.kart.kernel.ast.TreeVisitor
import eu.simonbinder.kart.kernel.utils.children

class Block (statements: List<Statement>): Statement() {

    val statements = children(statements)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitBlock(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        statements.forEach { it.accept(visitor) }
    }
}