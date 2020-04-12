package eu.simonbinder.kart.kernel.types

import eu.simonbinder.kart.kernel.Node
import eu.simonbinder.kart.kernel.Visitor
import eu.simonbinder.kart.kernel.utils.HasFlags
import eu.simonbinder.kart.kernel.utils.flag

class FunctionType(
    override val nullability: Nullability
): DartType {

    val typeParameters = mutableListOf<TypeParameter>()
    var requiredParameterCount = 0
    val positionalParameters = mutableListOf<DartType>()
    val namedParameters = mutableListOf<NamedDartType>()
    var returnType: DartType = DynamicType
    var typedef: TypedefType? = null

    override fun <T> accept(visitor: Visitor<T>): T {
        return visitor.visitFunctionType(this)
    }

    override fun <T> visitChildren(visitor: Visitor<T>) {
        typeParameters.forEach { it.accept(visitor) }
        positionalParameters.forEach { it.accept(visitor) }
        namedParameters.forEach { it.accept(visitor) }
        returnType.accept(visitor)
    }

}

/**
 * Wrapper around a [DartType] used as a named argument in a [FunctionType].
 */
class NamedDartType(var name: String, var type: DartType): HasFlags, Node {
    override var flags: Int = 0

    var isRequired by flag(0)

    override fun <T> accept(visitor: Visitor<T>): T {
        return visitor.visitNamedType(this)
    }

    override fun <T> visitChildren(visitor: Visitor<T>) {
        type.accept(visitor)
    }
}