package eu.simonbinder.kart.kernel.members.initializers

import eu.simonbinder.kart.kernel.TreeNode

abstract class Initializer : TreeNode() {
    var isSynthetic: Boolean = false
}