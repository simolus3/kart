package eu.simonbinder.kart.transformer.context

import eu.simonbinder.kart.kernel.statements.VariableDeclaration
import eu.simonbinder.kart.transformer.metadata.LoopMetaData
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol

data class InBodyCompilationContext(
    private val parent: CompilationContext,
    val variables: MutableMap<IrValueSymbol, VariableDeclaration> = mutableMapOf()
) : CompilationContext by parent