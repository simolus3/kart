package eu.simonbinder.kart.transformer

import eu.simonbinder.kart.kernel.TreeNode
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.name.Name

fun <T : TreeNode> T.withIrOffsets(ir: IrElement): T {
    fileOffset = ir.startOffset
    return this
}

val Name.identifierOrNull get() = if (isSpecial) null else identifier