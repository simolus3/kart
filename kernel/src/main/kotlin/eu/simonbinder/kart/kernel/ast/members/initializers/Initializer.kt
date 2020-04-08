package eu.simonbinder.kart.kernel.ast.members.initializers

import eu.simonbinder.kart.kernel.ast.TreeNode

abstract class Initializer : TreeNode() {
    var isSynthetic: Boolean = false
}