package eu.simonbinder.kart.kernel.members.initializers

import eu.simonbinder.kart.kernel.Reference
import eu.simonbinder.kart.kernel.TreeVisitor
import eu.simonbinder.kart.kernel.expressions.Arguments
import eu.simonbinder.kart.kernel.utils.child

abstract class BaseRedirectingInitializer(target: Reference?, arguments: Arguments?): Initializer() {

    lateinit var target: Reference
    var arguments by child(arguments)

    init {
        if (target != null) {
            this.target = target
        }
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        arguments.accept(visitor)
    }
}

class SuperInitializer(
    target: Reference? = null,
    arguments: Arguments? = null
): BaseRedirectingInitializer(target, arguments) {

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitSuperInitializer(this)
    }
}

class RedirectingInitializer(
    target: Reference? = null,
    arguments: Arguments? = null
): BaseRedirectingInitializer(target, arguments) {

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitRedirectingInitializer(this)
    }
}