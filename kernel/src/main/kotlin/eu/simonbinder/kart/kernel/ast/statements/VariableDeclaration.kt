package eu.simonbinder.kart.kernel.ast.statements

import eu.simonbinder.kart.kernel.ast.HasAnnotations
import eu.simonbinder.kart.kernel.ast.TreeVisitor
import eu.simonbinder.kart.kernel.ast.expressions.Expression
import eu.simonbinder.kart.kernel.types.DartType
import eu.simonbinder.kart.kernel.types.DynamicType
import eu.simonbinder.kart.kernel.utils.HasFlags
import eu.simonbinder.kart.kernel.utils.children
import eu.simonbinder.kart.kernel.utils.flag
import eu.simonbinder.kart.kernel.utils.nullableChild

class VariableDeclaration(
    var name: String? = null,
    var type: DartType = DynamicType,
    initializer: Expression? = null
) : Statement(), HasFlags, HasAnnotations {

    /**
     * The offset for the equal sign in the declaration (if it contains one)
     */
    var fileEqualsOffset: Int = NO_OFFSET

    override var flags: Int = 0

    var isFinal by flag(0)
    var isConst by flag(1)
    var isFieldFormal by flag(2)
    var isCovariant by flag(3)
    var isInScope by flag(4)
    var isGenericCovariantImpl by flag(5)
    var isLate by flag(6)
    var isRequired by flag(7)

    var initializer: Expression? by nullableChild(initializer)
    override val annotations = children<Expression>()

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitVariableDeclaration(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        initializer?.accept(visitor)
        annotations.forEach { it.accept(visitor) }
    }
}