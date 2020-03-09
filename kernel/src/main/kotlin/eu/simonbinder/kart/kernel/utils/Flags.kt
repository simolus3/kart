package eu.simonbinder.kart.kernel.utils

import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.reflect.KProperty

interface HasFlags {
    var flags: Byte
}

// flag 0..7, where 0 is the least significant bit

inline class PropertyFromFlag(private val index: Int) {
    operator fun getValue(thisRef: HasFlags, property: KProperty<*>): Boolean {
        return ((thisRef.flags.toInt() shr index) and 1) == 1
    }

    operator fun setValue(thisRef: HasFlags, property: KProperty<*>, value: Boolean) {
        thisRef.flags = if (value) {
            thisRef.flags or (1 shl index).toByte()
        } else {
            thisRef.flags and (1 shl index).inv().toByte()
        }
    }
}

fun HasFlags.flag(index: Int) = PropertyFromFlag(index)