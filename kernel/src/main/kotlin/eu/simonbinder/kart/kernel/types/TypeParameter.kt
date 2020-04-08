package eu.simonbinder.kart.kernel.types

import eu.simonbinder.kart.kernel.ast.TreeNode
import eu.simonbinder.kart.kernel.ast.TreeVisitor

class TypeParameter(
    val name: String? = null,
    val bound: DartType = DynamicType,
    val defaultType: DartType? = null
) : TreeNode() {

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitTypeParameter(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {}
}