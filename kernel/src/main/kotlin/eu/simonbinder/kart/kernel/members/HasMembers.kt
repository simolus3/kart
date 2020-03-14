package eu.simonbinder.kart.kernel.members

/**
 * Interface for Kernel nodes that have child members (libraries and classes).
 */
interface HasMembers {
    val members: MutableList<Member>
}

val HasMembers.constructors get() = members.filterIsInstance<Constructor>()
val HasMembers.procedures get() = members.filterIsInstance<Procedure>()
val HasMembers.fields get() = members.filterIsInstance<Field>()