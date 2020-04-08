package eu.simonbinder.kart.transformer

import eu.simonbinder.kart.kernel.ast.TreeNode
import eu.simonbinder.kart.kernel.types.DartType
import eu.simonbinder.kart.kernel.types.InterfaceType
import eu.simonbinder.kart.kernel.types.Nullability
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

fun <T : TreeNode> T.withIrOffsets(ir: IrElement): T {
    fileOffset = ir.startOffset
    return this
}

val Name.identifierOrNull get() = if (isSpecial) null else identifier

fun IrExpression.inBlockBody(): IrBlockBody {
    return if (this is IrBlock) {
        IrBlockBodyImpl(startOffset, endOffset, statements)
    } else {
        IrBlockBodyImpl(startOffset, endOffset, listOf(this))
    }
}

fun DartType.withNullabilityOfIr(type: IrType): DartType {
    if (this !is InterfaceType) return this

    val nullable = (type as? IrSimpleType)?.hasQuestionMark ?: false
    return withNullability(if (nullable) Nullability.NULLABLE else Nullability.NON_NULLABLE)
}