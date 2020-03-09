package eu.simonbinder.kart.kernel.expressions

import eu.simonbinder.kart.kernel.TreeVisitor
import eu.simonbinder.kart.kernel.utils.children

class StringConcatenation(
    expressions: List<Expression>? = null
): Expression() {

    val expressions = children(expressions)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitStringConcatenation(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        expressions.forEach {
            it.accept(visitor)
        }
    }

}