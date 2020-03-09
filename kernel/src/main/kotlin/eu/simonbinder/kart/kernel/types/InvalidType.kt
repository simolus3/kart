package eu.simonbinder.kart.kernel.types

import eu.simonbinder.kart.kernel.Visitor

object InvalidType : DartType {
    override val nullability: Nullability
        get() = throw NotImplementedError("InvalidType doesn't have nullability")

    override fun <T> accept(visitor: Visitor<T>): T {
        return visitor.visitInvalidType(this)
    }

    override fun <T> visitChildren(visitor: Visitor<T>) {}
}