package eu.simonbinder.kart.transformer

import eu.simonbinder.kart.kernel.TreeNode
import org.jetbrains.kotlin.ir.IrElement

fun <T : TreeNode> T.withIrOffsets(ir: IrElement): T {
    fileOffset = ir.startOffset
    return this
}