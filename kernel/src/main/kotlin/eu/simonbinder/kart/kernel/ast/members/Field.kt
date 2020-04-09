package eu.simonbinder.kart.kernel.ast.members

import eu.simonbinder.kart.kernel.Name
import eu.simonbinder.kart.kernel.Reference
import eu.simonbinder.kart.kernel.ast.TreeVisitor
import eu.simonbinder.kart.kernel.Uri
import eu.simonbinder.kart.kernel.ast.expressions.Expression
import eu.simonbinder.kart.kernel.types.DartType
import eu.simonbinder.kart.kernel.utils.HasFlags
import eu.simonbinder.kart.kernel.utils.flag
import eu.simonbinder.kart.kernel.utils.nullableChild

class Field(
    var name: Name,
    reference: Reference?,
    var type: DartType,
    initializer: Expression? = null,
    var fileUri: Uri? = null
) : Member(reference), HasFlags {

    override var flags: Int = 0

    var initializer: Expression? by nullableChild(initializer)

    var isFinal by flag(0)
    var isConst by flag(1)
    var isStatic by flag(2)
    var hasImplicitGetter by flag(3)
    var hasImplicitSetter by flag(4)
    var isCovariant by flag(5)
    var isGenericCovariantImpl by flag(6)
    var isLate by flag(7)
    var isExtensionMember by flag(8)
    var isNonNullableByDefault by flag(9)

    var fileEndOffset: Int = NO_OFFSET

    init {
        isNonNullableByDefault = true
    }

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitField(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        initializer?.accept(visitor)
    }

}