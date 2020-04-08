package eu.simonbinder.kart.kernel.ast.members

import eu.simonbinder.kart.kernel.*
import eu.simonbinder.kart.kernel.ast.FunctionNode
import eu.simonbinder.kart.kernel.ast.TreeVisitor
import eu.simonbinder.kart.kernel.utils.HasFlags
import eu.simonbinder.kart.kernel.utils.flag
import eu.simonbinder.kart.kernel.utils.nullableChild

enum class ProcedureKind {
    METHOD,
    GETTER,
    SETTER,
    OPERATOR,
    FACTORY
}

class Procedure(
    var kind: ProcedureKind,
    function: FunctionNode?,
    var name: Name? = null,
    reference: Reference? = null,
    var fileUri: Uri? = null
): Member(reference), HasFlags {

    /**
     * Offset of the start of the procedure, including any annotations.
     */
    var startFileOffset: Int = NO_OFFSET
    var fileEndOffset: Int = NO_OFFSET

    override var flags: Int = 0

    val function by nullableChild<FunctionNode?>(function)

    var isStatic by flag(0)
    var isAbstract by flag(1)
    var isExternal by flag(2)
    var isConst by flag(3)
    var isNonNullableByDefault by flag(10)

    init {
        isNonNullableByDefault = true
    }

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitProcedure(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        function?.accept(visitor)
    }
}