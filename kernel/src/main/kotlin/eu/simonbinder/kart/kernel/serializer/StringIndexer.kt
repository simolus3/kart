package eu.simonbinder.kart.kernel.serializer

class StringIndexer {

    private val index = mutableMapOf<String, Int>()
    private val storage = mutableListOf<String>()

    val strings: List<String> get() = storage

    init {
        // kernel library puts empty string at 0 index, let's do the same to be safe
        put("")
    }

    fun put(string: String): Int {
        if (index.containsKey(string)) return index[string]!!

        val length = storage.size
        index[string] = length
        storage.add(string)

        return length
    }

    operator fun get(string: String): Int? = index[string]
}