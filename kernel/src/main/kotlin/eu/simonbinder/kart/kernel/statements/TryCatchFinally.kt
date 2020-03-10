package eu.simonbinder.kart.kernel.statements

import eu.simonbinder.kart.kernel.TreeNode
import eu.simonbinder.kart.kernel.TreeVisitor
import eu.simonbinder.kart.kernel.types.DartType
import eu.simonbinder.kart.kernel.types.DynamicType
import eu.simonbinder.kart.kernel.utils.*

class TryCatch (
    body: Statement? = null,
    catches: List<Catch>? = null
): Statement(), HasFlags {

    override var flags: Byte = 0

    var body by child(body)
    val catches = children(catches)

    var anyCatchNeedsStackTrace by flag(0)
    var isSynthesized by flag(1)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitTryCatch(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        body.accept(visitor)
        catches.forEach { it.accept(visitor) }
    }

    fun computeNeedsStackTrace() {
        anyCatchNeedsStackTrace = catches.any { it.stackTrace != null }
    }
}

class Catch(
    var guard: DartType = DynamicType,
    exception: VariableDeclaration? = null,
    stackTrace: VariableDeclaration? = null,
    body: Statement? = null
) : TreeNode() {

    var exception by nullableChild<VariableDeclaration?>(exception)
    var stackTrace by nullableChild<VariableDeclaration?>(stackTrace)

    var body by child(body)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitCatch(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        exception?.accept(visitor)
        stackTrace?.accept(visitor)
        body.accept(visitor)
    }
}

class TryFinally(
    body: Statement? = null,
    finalizer: Statement? = null
) : Statement() {

    var body by child(body)
    var finalizer by child(finalizer)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitTryFinally(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        body.accept(visitor)
        finalizer.accept(visitor)
    }

}