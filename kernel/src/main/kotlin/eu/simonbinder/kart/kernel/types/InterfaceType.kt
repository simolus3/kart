package eu.simonbinder.kart.kernel.types

import eu.simonbinder.kart.kernel.Reference
import eu.simonbinder.kart.kernel.Visitor

class InterfaceType(
    override val nullability: Nullability = Nullability.NON_NULLABLE,
    var classReference: Reference? = null,
    val typeArguments: MutableList<DartType> = mutableListOf()
) : DartType {

    override fun <T> accept(visitor: Visitor<T>): T {
        return visitor.visitInterfaceType(this)
    }

    override fun <T> visitChildren(visitor: Visitor<T>) {
        typeArguments.forEach { it.accept(visitor) }
    }

    fun withNullability(nullability: Nullability = this.nullability): InterfaceType =
        if (nullability == this.nullability)
            this
        else
            InterfaceType(nullability, classReference, typeArguments.toMutableList())
}