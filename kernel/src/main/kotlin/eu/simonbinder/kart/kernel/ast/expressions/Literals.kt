package eu.simonbinder.kart.kernel.ast.expressions

import eu.simonbinder.kart.kernel.ast.TreeVisitor

sealed class Literal<T> : Expression() {
    abstract val value: T

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {}
}

class StringLiteral(override val value: String): Literal<String>() {
    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitStringLiteral(this)
    }
}

class IntegerLiteral(override val value: Long): Literal<Long>() {
    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitIntegerLiteral(this)
    }
}

class DoubleLiteral(override val value: Double): Literal<Double>() {
    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitDoubleLiteral(this)
    }
}

class BooleanLiteral(override val value: Boolean): Literal<Boolean>() {
    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitBooleanLiteral(this)
    }
}

class NullLiteral: Literal<Nothing?>() {
    override val value: Nothing?
        get() = null

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitNullLiteral(this)
    }
}

