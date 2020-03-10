package eu.simonbinder.kart.kernel

import eu.simonbinder.kart.kernel.statements.Statement
import eu.simonbinder.kart.kernel.statements.VariableDeclaration
import eu.simonbinder.kart.kernel.types.DartType
import eu.simonbinder.kart.kernel.types.TypeParameter
import eu.simonbinder.kart.kernel.types.VoidType
import eu.simonbinder.kart.kernel.utils.children
import eu.simonbinder.kart.kernel.utils.nullableChild

enum class AsyncMarker {
    Sync,
    SyncStar,
    Async,
    AsyncStar,
    SyncYielding
}

class FunctionNode(
    typeParameters: List<TypeParameter>? = null,
    positionalParameters: List<VariableDeclaration>? = null,
    namedParameters: List<VariableDeclaration>? = null,
    val returnType: DartType = VoidType,
    var asyncMarker: AsyncMarker = AsyncMarker.Sync,
    body: Statement?
) : TreeNode() {

    var endOffset: Int = NO_OFFSET

    val typeParameters = children(typeParameters)
    val positionalParameters = children(positionalParameters)
    val namedParameters = children(namedParameters)
    val body by nullableChild<Statement?>(body)

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitFunctionNode(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        typeParameters.forEach { it.accept(visitor) }
        positionalParameters.forEach { it.accept(visitor) }
        namedParameters.forEach { it.accept(visitor) }
        body?.accept(visitor)
    }
}