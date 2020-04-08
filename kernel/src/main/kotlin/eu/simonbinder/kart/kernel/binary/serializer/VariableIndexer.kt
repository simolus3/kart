package eu.simonbinder.kart.kernel.binary.serializer

import eu.simonbinder.kart.kernel.ast.statements.VariableDeclaration
import java.util.*

/**
 * Finds the index for a [VariableDeclaration], which is important for serialization.
 *
 * In the serialized file, variables are referred to by their index in the scope
 * in which they were declared.
 * So, 0 would be first variable declared in the outermost scope. Larger numbers are
 * variables declared later in the given scope, or in a more deeply nested scope.
 *
 * Function parameters are indexed from left to right and make up the outermost scope
 * (enclosing the functions's body). Variables ARE NOT in scope in their own initializer.
 * Variables ARE NOT in scope before their declaration. Variables ARE in scope in nested
 * lambdas.
 *
 * When declared, a variable remains in scope until the end of the immediately enclosing
 * Block, Let, FunctionNode, ForStatement, ForInStatement or Catch.
 */
class VariableIndexer {

    private val index = mutableMapOf<VariableDeclaration, Int>()
    private val scopes = Stack<Int>()

    private var stackHeight = 0

    fun declare(node: VariableDeclaration) {
        index[node] = stackHeight++
    }

    fun pushScope() {
        scopes.push(stackHeight)
    }

    fun popScope() {
        stackHeight = scopes.pop()
    }

    fun restoreScope(variableCount: Int) {
        stackHeight += variableCount
    }

    operator fun get(node: VariableDeclaration) = index[node]

}