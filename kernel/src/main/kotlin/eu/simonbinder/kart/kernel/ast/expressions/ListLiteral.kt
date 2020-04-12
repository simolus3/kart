package eu.simonbinder.kart.kernel.ast.expressions

import eu.simonbinder.kart.kernel.Visitor
import eu.simonbinder.kart.kernel.ast.TreeVisitor
import eu.simonbinder.kart.kernel.types.DartType
import eu.simonbinder.kart.kernel.types.InvalidType
import eu.simonbinder.kart.kernel.utils.children

class ListLiteral(
    var typeArgument: DartType = InvalidType,
    values: List<Expression>? = null
): Expression() {

    val values = children(values)
    var isConst: Boolean = false

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitListLiteral(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        typeArgument.accept(visitor as Visitor<T>)
        values.forEach { it.accept(visitor) }
    }
}