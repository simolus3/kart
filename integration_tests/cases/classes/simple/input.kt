package input

class TreeNode(
    val data: Long,
    val left: TreeNode?,
    val right: TreeNode?
)

fun main() {
    val x = TreeNode(2, null, null)
    println(x.data)
}