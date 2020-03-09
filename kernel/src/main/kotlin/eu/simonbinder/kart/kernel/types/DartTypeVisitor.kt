package eu.simonbinder.kart.kernel.types

interface DartTypeVisitor<R> {

    fun defaultDartType(node: DartType): R

    fun visitInvalidType(node: InvalidType): R = defaultDartType(node)
    fun visitBottomType(node: BottomType): R = defaultDartType(node)
    fun visitInterfaceType(node: InterfaceType): R = defaultDartType(node)
    fun visitDynamicType(node: DynamicType): R = defaultDartType(node)
    fun visitVoidType(node: VoidType): R = defaultDartType(node)
    fun visitNeverType(node: NeverType): R = defaultDartType(node)
}