package eu.simonbinder.kart.kernel

import eu.simonbinder.kart.kernel.members.Field
import eu.simonbinder.kart.kernel.members.Procedure

/**
 *  A string sequence that identifies a library, class, or member.
 *
 * Canonical names are organized in a prefix tree.  Each node knows its
 * parent, children, and the AST node it is currently bound to.
 *
 * The following schema specifies how the canonical name of a given object
 * is defined:
 *
 *      Library:
 *         URI of library
 *
 *      Class:
 *         Canonical name of enclosing library
 *         Name of class
 *
 *      Extension:
 *         Canonical name of enclosing library
 *         Name of extension
 *
 *      Constructor:
 *         Canonical name of enclosing class or library
 *         "@constructors"
 *         Qualified name
 *
 *      Field:
 *         Canonical name of enclosing class or library
 *         "@fields"
 *         Qualified name
 *
 *      Typedef:
 *         Canonical name of enclosing class
 *         "@typedefs"
 *         Name text
 *
 *      Procedure that is not an accessor or factory:
 *         Canonical name of enclosing class or library
 *         "@methods"
 *         Qualified name
 *
 *      Procedure that is a getter:
 *         Canonical name of enclosing class or library
 *         "@getters"
 *         Qualified name
 *
 *      Procedure that is a setter:
 *         Canonical name of enclosing class or library
 *         "@setters"
 *         Qualified name
 *
 *      Procedure that is a factory:
 *         Canonical name of enclosing class
 *         "@factories"
 *         Qualified name
 *
 *      Qualified name:
 *         if private: URI of library
 *         Name text
 *
 * The "qualified name" allows a member to have a name that is private to
 * a library other than the one containing that member.
*/
class CanonicalName private constructor(
    val parent: CanonicalName?,
    val name: String
) {
    var reference: Reference? = null
    private val childByName: MutableMap<String, CanonicalName> = mutableMapOf()

    val isRoot get() = parent == null
    val children get() = childByName.values

    fun hasChild(name: String) = childByName.containsKey(name)

    fun removeChild(name: String) = childByName.remove(name)

    fun getChild(name: String) = childByName.computeIfAbsent(name) {
        CanonicalName(this, name)
    }

    fun getSubChild(names: Iterable<String>) = names.fold(this) { qualified, name ->
        qualified.getChild(name)
    }

    fun bindTo(target: Reference) {
        if (reference == target) return;
        if (reference != null) {
            throw IllegalStateException("$this is already bound")
        }
        target.canonicalName?.let { bound ->
            throw IllegalStateException("Cannot bind $this to ${target.node}, target is already bound to $bound")
        }
        target.canonicalName = this
        reference = target
    }

    fun unbind() {
        reference?.let {
            assert(it.canonicalName === this)
            it.canonicalName = null
        }
        reference = null
    }

    override fun toString(): String {
        return when {
            isRoot -> "<root>"
            parent!!.isRoot -> name
            else -> "$parent.$name"
        }
    }

    companion object {
        fun root(): CanonicalName {
            return CanonicalName(null, "")
        }
    }
}

/**
 * A level of indirection between a reference and its definition.
 */
class Reference(var canonicalName: CanonicalName? = null) {

    lateinit var node: NamedNode

    val asProcedure: Procedure? get() = node as Procedure
    val asField: Field? get() = node as Field
}

fun CanonicalName.asReference() = Reference(this)

class Name(
    val name: String,
    val libraryReference: Reference? = null
): Node {

    val isPrivate: Boolean = name.startsWith("_")

    override fun <T> accept(visitor: Visitor<T>): T {
        return visitor.visitName(this)
    }

    override fun <T> visitChildren(visitor: Visitor<T>) { }
}