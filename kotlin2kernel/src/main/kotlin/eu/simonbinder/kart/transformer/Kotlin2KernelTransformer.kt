package eu.simonbinder.kart.transformer

import eu.simonbinder.kart.kernel.members.Component
import eu.simonbinder.kart.kotlin.DartBackendContext
import eu.simonbinder.kart.transformer.compilers.ModuleCompiler
import eu.simonbinder.kart.transformer.context.GlobalCompilationContext
import eu.simonbinder.kart.transformer.names.Names
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class Kotlin2KernelTransformer(private val dartBackendContext: DartBackendContext) {
    val component = Component()

    private val names = Names(dartBackendContext)

    private fun createContext() = GlobalCompilationContext(CompilationInfo(names, component, dartBackendContext))

    fun addModule(input: IrModuleFragment) {
        val context = createContext()
        input.accept(ModuleCompiler, context)

        context.info.visitedFiles.values.forEach { source ->
            source.fileUri?.let { uri ->
                component.sources[uri] = source
            }
        }
    }
}