package eu.simonbinder.kart.kernel.ast.members

import eu.simonbinder.kart.kernel.*
import eu.simonbinder.kart.kernel.ast.FunctionNode
import eu.simonbinder.kart.kernel.ast.TreeVisitor
import eu.simonbinder.kart.kernel.ast.expressions.Expression
import eu.simonbinder.kart.kernel.ast.members.initializers.Initializer
import eu.simonbinder.kart.kernel.utils.HasFlags
import eu.simonbinder.kart.kernel.utils.child
import eu.simonbinder.kart.kernel.utils.children
import eu.simonbinder.kart.kernel.utils.flag

class Constructor(
    reference: Reference? = null,
    var name: Name,
    var fileUri: Uri? = null,
    function: FunctionNode? = null
) : Member(reference), HasFlags {

    var function by child(function)
    val initializers = children<Initializer>()
    override val annotations = children<Expression>()

    var fileStartOffset: Int = NO_OFFSET
    var fileEndOffset: Int = NO_OFFSET

    override var flags: Int = 0
    var isConst by flag(0)
    var isExternal by flag(1)
    var isSynthetic by flag(2)
    var isNonNullableByDefault by flag(3)

    init {
        isNonNullableByDefault = true
    }

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitConstructor(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        function.accept(visitor)
        initializers.forEach { it.accept(visitor) }
        annotations.forEach { it.accept(visitor) }
    }
}