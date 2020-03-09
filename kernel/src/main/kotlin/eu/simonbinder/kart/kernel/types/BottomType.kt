package eu.simonbinder.kart.kernel.types

import eu.simonbinder.kart.kernel.Visitor

object BottomType : DartType {

    override val nullability: Nullability
        get() = Nullability.NON_NULLABLE

    override fun <T> accept(visitor: Visitor<T>): T {
        return visitor.visitBottomType(this)
    }

    override fun <T> visitChildren(visitor: Visitor<T>) {}
}