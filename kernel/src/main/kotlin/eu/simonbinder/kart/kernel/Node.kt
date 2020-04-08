package eu.simonbinder.kart.kernel

interface Node {
    fun <T> accept(visitor: Visitor<T>): T
    fun <T> visitChildren(visitor: Visitor<T>)
}
