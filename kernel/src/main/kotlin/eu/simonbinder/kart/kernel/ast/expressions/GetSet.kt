package eu.simonbinder.kart.kernel.ast.expressions

import eu.simonbinder.kart.kernel.Name
import eu.simonbinder.kart.kernel.Reference
import eu.simonbinder.kart.kernel.ast.TreeVisitor
import eu.simonbinder.kart.kernel.utils.child

class StaticGet(
    val reference: Reference?
) : Expression() {

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitStaticGet(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {}
}

class StaticSet(
    val reference: Reference,
    value: Expression? = null
) : Expression() {

    var value by child(value)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitStaticSet(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        value.accept(visitor)
    }
}

class PropertyGet(
    receiver: Expression? = null,
    var name: Name,
    var interfaceTarget: Reference? = null
): Expression() {

    var receiver: Expression by child(receiver)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitPropertyGet(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        receiver.accept(visitor)
    }
}

class PropertySet(
    receiver: Expression? = null,
    var name: Name,
    value: Expression? = null,
    var interfaceTarget: Reference? = null
): Expression() {
    var receiver: Expression by child(receiver)
    var value: Expression by child(value)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitPropertySet(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        receiver.accept(visitor)
        value.accept(visitor)
    }
}