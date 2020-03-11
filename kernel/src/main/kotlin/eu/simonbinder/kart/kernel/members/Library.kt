package eu.simonbinder.kart.kernel.members

import eu.simonbinder.kart.kernel.*
import eu.simonbinder.kart.kernel.utils.HasFlags
import eu.simonbinder.kart.kernel.utils.children
import eu.simonbinder.kart.kernel.utils.flag

class Library(
    members: MutableList<Member>? = null,
    val dartVersion: DartVersion = DartVersion.LATEST,
    reference: Reference? = null,
    var fileUri: Uri? = null
) : NamedNode(reference), HasFlags {

    override var flags: Byte = 0

    var isSynthetic by flag(1)
    var isNonNullableByDefault by flag(2)

    val members = children(members)
    var name: String? = null

    /**
     * All sources that contributed to this library.
     */
    val sourceUris = mutableListOf<Uri>()

    init {
        isNonNullableByDefault = true
    }

    override fun <T> accept(visitor: TreeVisitor<T>): T {
        return visitor.visitLibrary(this)
    }

    override fun <T> visitChildren(visitor: TreeVisitor<T>) {
        members.forEach { it.accept(visitor) }
    }

}