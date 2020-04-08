package eu.simonbinder.kart.kernel.binary.serializer

import eu.simonbinder.kart.kernel.CanonicalName

class NameIndexer {

    private val index = mutableMapOf<CanonicalName, Int>()
    private val names = mutableListOf<CanonicalName>()

    fun listNames(): List<CanonicalName> = names

    operator fun get(name: CanonicalName?): Int {
        if (name == null || name.isRoot) return 0

        // Make sure the parent name has been indexed as well. Even though that's not
        // specified, Dart Kernel tools expect the parent to have a lower index.
        get(name.parent)

        var indexOfName = index[name]
        if (indexOfName == null) {
            indexOfName = names.size
            index[name] = indexOfName
            names.add(name)
        }
        return indexOfName + 1
    }

}