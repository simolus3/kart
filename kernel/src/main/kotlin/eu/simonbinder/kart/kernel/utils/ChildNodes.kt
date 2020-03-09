package eu.simonbinder.kart.kernel.utils

import eu.simonbinder.kart.kernel.TreeNode
import kotlin.reflect.KProperty

fun <T : TreeNode?> TreeNode.child(): ChildNodeDelegate<T> {
    return ChildNodeDelegate()
}

fun <T: TreeNode> TreeNode.child(initial: T? = null): ChildNodeDelegate<T> {
    return if (initial == null) {
        ChildNodeDelegate()
    } else {
        ChildNodeDelegate(this, initial)
    }
}

fun <T: TreeNode?> TreeNode.nullableChild(initial: T? = null): ChildNodeDelegate<T> {
    return if (initial == null) {
        ChildNodeDelegate<T>().also {
            it.isInitialized = true
        }
    } else {
        ChildNodeDelegate(this, initial)
    }
}

class ChildList<T: TreeNode>(
    val parent: TreeNode
) : AbstractMutableList<T>() {

    private val items: MutableList<T> = mutableListOf()

    override val size: Int
        get() = items.size

    override fun add(index: Int, element: T) {
        element.parent = parent
        items.add(index, element)
    }

    override fun get(index: Int): T = items[index]

    override fun removeAt(index: Int): T = items.removeAt(index)

    override fun set(index: Int, element: T): T {
        element.parent = parent
        return items.set(index, element)
    }
}

fun <T: TreeNode> TreeNode.children(initial: List<T>? = null): ChildList<T> {
    return ChildList<T>(this).also {
        if (initial != null) it.addAll(initial)
    }
}

class ChildNodeDelegate<T : TreeNode?> {

    private var value: T? = null
    var isInitialized = false

    constructor()

    constructor(thisRef: TreeNode, initial: T) {
        initial?.parent = thisRef
        value = initial
        isInitialized = true
    }

    operator fun getValue(thisRef: TreeNode, property: KProperty<*>): T {
        if (!isInitialized) {
            throw IllegalStateException("Child node not initialized")
        } else {
            @Suppress("UNCHECKED_CAST")
            return value as T
        }
    }

    operator fun setValue(thisRef: TreeNode, property: KProperty<*>, value: T) {
        this.value?.parent = null
        this.value = value
        value?.parent = thisRef
        isInitialized = true
    }

}