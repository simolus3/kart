package eu.simonbinder.kart.kernel

import eu.simonbinder.kart.kernel.types.DartType
import eu.simonbinder.kart.kernel.types.DartTypeVisitor

interface Visitor<R> : TreeVisitor<R>, DartTypeVisitor<R> {

    fun defaultNode(node: Node): R

    override fun defaultTreeNode(node: TreeNode): R = defaultNode(node)
    override fun defaultDartType(node: DartType): R = defaultNode(node)

    fun visitName(node: Name): R = defaultNode(node)
}