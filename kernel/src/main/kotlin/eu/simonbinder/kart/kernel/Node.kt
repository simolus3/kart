package eu.simonbinder.kart.kernel

interface Node {
    fun <T> accept(visitor: Visitor<T>): T
    fun <T> visitChildren(visitor: Visitor<T>)
}

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

    val reference: Reference = reference ?: Reference()

    val canonicalName: CanonicalName? get() = reference.canonicalName

    init {
        this.reference.node = this
    }

}

