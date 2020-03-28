package input

class BinaryTree(
    val value: Long,
    val left: BinaryTree?,
    val right: BinaryTree?
)

tailrec fun BinaryTree.search(key: Long): BinaryTree? = when {
    value == key -> this
    key < value -> left?.search(key)
    else -> right?.search(key)
}

fun main() {
    val node = BinaryTree(
        3,
        BinaryTree(2, null, null),
        BinaryTree(4, null, null)
    )

    println(node.search(6))
}