package eu.simonbinder.kart.kernel.ast.members

import eu.simonbinder.kart.kernel.*
import eu.simonbinder.kart.kernel.ast.TreeNode
import eu.simonbinder.kart.kernel.ast.TreeVisitor
import eu.simonbinder.kart.kernel.utils.children

class Component(
    libraries: MutableList<Library>? = null,
    val dartVersion: DartVersion = DartVersion.DEFAULT_FOR_OUTPUT
) : TreeNode() {

    val libraries = children(libraries)
    var mainMethodReference: Reference? = null
    var nonNullableByDefaultCompiledMode = NonNullableByDefaultCompiledMode.Strong

    val sources = mutableMapOf<Uri, Source>()

    var mainMethod: Procedure?
        get() = mainMethodReference?.asProcedure
        set(value) {
            mainMethodReference = value?.reference
        }

    fun createLibrary(reference: Reference? = null): Library {
        return Library(
            dartVersion = dartVersion,
            reference = reference
        ).also {
            it.nonNullableByDefaultCompiledMode = NonNullableByDefaultCompiledMode.Strong
            libraries += it
        }
    }

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitComponent(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        libraries.forEach { it.accept(visitor) }
    }

}