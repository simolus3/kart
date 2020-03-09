package eu.simonbinder.kart.kernel.members

interface MemberVisitor<R> {

    fun defaultMember(node: Member): R

    fun visitProcedure(node: Procedure): R = defaultMember(node)
}