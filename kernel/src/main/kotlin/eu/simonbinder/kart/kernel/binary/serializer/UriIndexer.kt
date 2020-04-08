package eu.simonbinder.kart.kernel.binary.serializer

import eu.simonbinder.kart.kernel.Uri

class UriIndexer {

    private val index = mutableMapOf<Uri?, Int>()

    val length get() = index.size
    val keys get(): Set<Uri?> = index.keys

    init {
        put(null)
    }

    fun put(uri: Uri?): Int = index.computeIfAbsent(uri) {
        index.size
    }
}