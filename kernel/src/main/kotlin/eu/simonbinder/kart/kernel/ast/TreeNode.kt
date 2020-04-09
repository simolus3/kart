package eu.simonbinder.kart.kernel.ast

import eu.simonbinder.kart.kernel.*
import eu.simonbinder.kart.kernel.ast.expressions.Expression

abstract class TreeNode : Node {

    var parent: TreeNode? = null
    var fileOffset: Int = NO_OFFSET

    abstract fun <T> accept(visitor: TreeVisitor<T>): T
    abstract fun <T> visitChildren(visitor: TreeVisitor<T>)

    override fun <T> accept(visitor: Visitor<T>): T = accept(visitor as TreeVisitor<T>)
    override fun <T> visitChildren(visitor: Visitor<T>) = visitChildren(visitor as TreeVisitor<T>)

    companion object {
        const val NO_OFFSET = -1
    }
}

abstract class NamedNode(reference: Reference? = null) : TreeNode() {

    var reference: Reference = reference ?: Reference()
        private set

    var canonicalName: CanonicalName?
        get() = reference.canonicalName
        set(value) {
            canonicalName?.unbind()

            if (value != null) {
                reference = value.asReference().also { it.node = this }
            }
        }

    init {
        this.reference.node = this
    }
}

interface HasAnnotations {
    val annotations: MutableList<Expression>
}