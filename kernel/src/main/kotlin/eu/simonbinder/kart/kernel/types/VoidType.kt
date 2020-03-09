package eu.simonbinder.kart.kernel.types

import eu.simonbinder.kart.kernel.Visitor

object VoidType : DartType {
    override val nullability: Nullability
        get() = Nullability.NULLABLE

    override fun <T> accept(visitor: Visitor<T>): T {
        return visitor.visitVoidType(this)
    }

    override fun <T> visitChildren(visitor: Visitor<T>) {}
}