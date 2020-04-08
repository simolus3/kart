package eu.simonbinder.kart.kernel.ast.members

interface MemberVisitor<R> {

    fun defaultMember(node: Member): R

    fun visitConstructor(node: Constructor): R = defaultMember(node)
    fun visitField(node: Field): R = defaultMember(node)
    fun visitProcedure(node: Procedure): R = defaultMember(node)
}