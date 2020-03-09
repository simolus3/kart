package eu.simonbinder.kart.kernel.members

import eu.simonbinder.kart.kernel.*
import eu.simonbinder.kart.kernel.utils.child

enum class ProcedureKind {
    METHOD,
    GETTER,
    SETTER,
    OPERATOR,
    FACTORY
}

class Procedure(
    val kind: ProcedureKind,
    function: FunctionNode,
    val name: Name? = null,
    reference: Reference? = null,
    var fileUri: Uri? = null
): Member(reference) {

    /**
     * Offset of the start of the procedure, including any annotations.
     */
    var startFileOffset: Int = NO_OFFSET
    var fileEndOffset: Int = NO_OFFSET

    val function by child(function)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitProcedure(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        function.accept(visitor)
    }
}