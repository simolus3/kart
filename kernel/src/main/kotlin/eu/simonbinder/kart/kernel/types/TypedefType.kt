package eu.simonbinder.kart.kernel.types

import eu.simonbinder.kart.kernel.Reference
import eu.simonbinder.kart.kernel.Visitor

class TypedefType(
    override val nullability: Nullability,
    var reference: Reference? = null,
    val typeArguments: MutableList<DartType> = mutableListOf()
): DartType {

    override fun <T> accept(visitor: Visitor<T>): T {
        return visitor.visitTypedefType(this)
    }

    override fun <T> visitChildren(visitor: Visitor<T>) {
        typeArguments.forEach { it.accept(visitor) }
    }

}