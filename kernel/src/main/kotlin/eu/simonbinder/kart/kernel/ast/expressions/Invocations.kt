package eu.simonbinder.kart.kernel.ast.expressions

import eu.simonbinder.kart.kernel.*
import eu.simonbinder.kart.kernel.ast.TreeNode
import eu.simonbinder.kart.kernel.ast.TreeVisitor
import eu.simonbinder.kart.kernel.utils.child
import eu.simonbinder.kart.kernel.ast.members.Procedure
import eu.simonbinder.kart.kernel.types.DartType

class Arguments(
    val typeParameters: List<DartType> = emptyList(),
    val positional: List<Expression> = emptyList(),
    val named: List<NamedExpression> = emptyList()
): TreeNode() {

    init {
        positional.forEach { it.parent = this }
        positional.forEach { it.parent = this }
    }

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitArguments(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        positional.forEach {
            it.accept(visitor)
        }
        named.forEach {
            it.accept(visitor)
        }
    }

}

class NamedExpression(val name: String, expression: Expression): Expression() {

    var expression: Expression by child(expression)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitNamedExpression(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        expression.accept(visitor)
    }

}

class StaticInvocation(
    val reference: Reference,
    val arguments: Arguments
) : Expression() {

    constructor(procedure: Procedure, arguments: Arguments) : this(procedure.reference, arguments)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitStaticInvocation(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        arguments.accept(visitor)
    }
}

class MethodInvocation(
    receiver: Expression? = null,
    var name: Name,
    arguments: Arguments? = null,
    var reference: Reference? = null
) : Expression() {

    var receiver: Expression by child(receiver)
    var arguments: Arguments by child(arguments)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitMethodInvocation(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        receiver.accept(visitor)
        arguments.accept(visitor)
    }
}

class ConstructorInvocation(
    var reference: Reference? = null,
    arguments: Arguments? = null
): Expression() {

    var arguments: Arguments by child(arguments)
    var isConstant: Boolean = false

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitConstructorInvocation(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        arguments.accept(visitor)
    }
}