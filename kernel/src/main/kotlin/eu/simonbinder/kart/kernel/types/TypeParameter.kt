package eu.simonbinder.kart.kernel.types

import eu.simonbinder.kart.kernel.Visitor
import eu.simonbinder.kart.kernel.ast.HasAnnotations
import eu.simonbinder.kart.kernel.ast.TreeNode
import eu.simonbinder.kart.kernel.ast.TreeVisitor
import eu.simonbinder.kart.kernel.ast.expressions.Expression
import eu.simonbinder.kart.kernel.utils.HasFlags
import eu.simonbinder.kart.kernel.utils.children
import eu.simonbinder.kart.kernel.utils.flag

class TypeParameter(
    val name: String? = null,
    val bound: DartType = DynamicType,
    val defaultType: DartType? = null
) : TreeNode(), HasAnnotations, HasFlags {

    override var flags: Int = 0
    override val annotations = children<Expression>()

    var isGenericCovariantImpl by flag(0)
    var variance: Variance = Variance.INVARIANT

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitTypeParameter(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {}
}

class TypeParameterType(
    override val nullability: Nullability,
    val parameter: TypeParameter,
    var bound: DartType?
): DartType {

    override fun <T> accept(visitor: Visitor<T>): T {
        return visitor.visitTypeParameterType(this)
    }

    override fun <T> visitChildren(visitor: Visitor<T>) {
        parameter.accept(visitor)
        bound?.accept(visitor)
    }

}