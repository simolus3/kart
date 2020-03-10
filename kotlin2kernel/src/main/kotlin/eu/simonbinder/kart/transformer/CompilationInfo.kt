package eu.simonbinder.kart.transformer

import eu.simonbinder.kart.kernel.Source
import eu.simonbinder.kart.kernel.Uri
import eu.simonbinder.kart.kernel.members.Component
import eu.simonbinder.kart.kernel.types.DartType
import eu.simonbinder.kart.kotlin.DartBackendContext
import eu.simonbinder.kart.transformer.invoke.DartIntrinsics
import eu.simonbinder.kart.transformer.metadata.MetadataRepository
import eu.simonbinder.kart.transformer.names.Names
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.types.IrType
import java.io.File

class CompilationInfo(
    val names: Names,
    val component: Component,
    dartBackendContext: DartBackendContext,
    val visitedFiles: MutableMap<IrFileSymbol, Source> = mutableMapOf()
) {

    val irBuiltIns = dartBackendContext.irBuiltIns
    val dartIntrinsics = DartIntrinsics(names.dartNames, irBuiltIns, dartBackendContext.symbolTable)

    val meta = MetadataRepository()

    fun loadFile(file: IrFile): Uri {
        val symbol = file.symbol
        if (visitedFiles.containsKey(symbol)) visitedFiles[symbol]!!

        val content = File(file.path).readText(Charsets.UTF_8)
        val uri = Uri.file(file.path)
        visitedFiles[symbol] = Source(content, importUri = null, fileUri = uri)
        return uri
    }

    fun dartTypeFor(irType: IrType): DartType {
        return dartIntrinsics.intrinsicType(irType)
    }
}