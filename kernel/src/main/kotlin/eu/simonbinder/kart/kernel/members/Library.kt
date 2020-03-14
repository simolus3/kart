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
) : NamedNode(reference), HasFlags, HasMembers {

    override var flags: Int = 0

    // note: flag(0) is unused
    var isSynthetic by flag(1)
    var isNonNullableByDefault by flag(2)
    private var nnbdModeBit1 by flag(3)
    private var nnbdModeBit2 by flag(4)

    var nonNullableByDefaultCompiledMode: NonNullableByDefaultCompiledMode
    get() {
        val weak = nnbdModeBit1
        val strong = nnbdModeBit2

        return when {
            weak && strong -> NonNullableByDefaultCompiledMode.Agnostic
            strong -> NonNullableByDefaultCompiledMode.Strong
            weak -> NonNullableByDefaultCompiledMode.Weak
            else -> NonNullableByDefaultCompiledMode.Disabled
        }
    }
    set(value) {
        val modeBits = when (value) {
            NonNullableByDefaultCompiledMode.Disabled -> false to false
            NonNullableByDefaultCompiledMode.Weak -> true to false
            NonNullableByDefaultCompiledMode.Strong -> false to true
            NonNullableByDefaultCompiledMode.Agnostic -> true to true
        }
        nnbdModeBit1 = modeBits.first
        nnbdModeBit2 = modeBits.second
    }

    override val members = children(members)
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

enum class NonNullableByDefaultCompiledMode {
    Disabled,
    Weak,
    Strong,
    Agnostic
}