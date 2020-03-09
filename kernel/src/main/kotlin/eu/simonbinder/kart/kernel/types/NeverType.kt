package eu.simonbinder.kart.kernel.types

import eu.simonbinder.kart.kernel.Visitor

class NeverType(override val nullability: Nullability) : DartType {

    override fun hashCode() = nullability.hashCode() * 1337

    override fun equals(other: Any?): Boolean {
        return other is NeverType && other.nullability == nullability
    }

    override fun <T> accept(visitor: Visitor<T>): T {
        return visitor.visitNeverType(this)
    }

    override fun <T> visitChildren(visitor: Visitor<T>) {}
}