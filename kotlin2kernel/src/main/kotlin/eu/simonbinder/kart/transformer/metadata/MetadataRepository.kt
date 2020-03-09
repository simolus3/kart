package eu.simonbinder.kart.transformer.metadata

import org.jetbrains.kotlin.ir.expressions.IrLoop

class MetadataRepository {

    private val loopData: MutableMap<IrLoop, LoopMetaData> = mutableMapOf()

    fun metaForLoop(loop: IrLoop) = loopData.computeIfAbsent(loop) { LoopMetaData() }

}