package eu.simonbinder.kart.kernel.serializer

class SimpleStackBasedIndexer<Key> {

    private val index: MutableMap<Key, UInt> = mutableMapOf()
    var stackHeight = 0u

    fun enter(node: Key) {
        index[node] = stackHeight++
    }

    fun exit() {
        stackHeight--
    }

    operator fun get(node: Key): UInt {
        val found = index[node] ?: throw IllegalArgumentException("Node not in index: $node")

        if (stackHeight < found) {
            throw IllegalArgumentException("Tried to access node from outer scope")
        }

        return found
    }
}