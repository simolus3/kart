package eu.simonbinder.kart.kernel.utils

import kotlin.reflect.KProperty

interface HasFlags {
    var flags: Int
}

inline class PropertyFromFlag(private val index: Int) {
    operator fun getValue(thisRef: HasFlags, property: KProperty<*>): Boolean {
        return ((thisRef.flags shr index) and 1) == 1
    }

    operator fun setValue(thisRef: HasFlags, property: KProperty<*>, value: Boolean) {
        thisRef.flags = if (value) {
            thisRef.flags or (1 shl index)
        } else {
            thisRef.flags and (1 shl index).inv()
        }
    }
}

fun HasFlags.flag(index: Int) = PropertyFromFlag(index)