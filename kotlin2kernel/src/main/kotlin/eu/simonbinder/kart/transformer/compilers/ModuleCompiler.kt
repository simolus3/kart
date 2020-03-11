package eu.simonbinder.kart.transformer.compilers

import eu.simonbinder.kart.transformer.context.GlobalCompilationContext
import eu.simonbinder.kart.transformer.context.names
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

object ModuleCompiler : IrElementVisitor<Unit, GlobalCompilationContext> {
    override fun visitElement(element: IrElement, data: GlobalCompilationContext) {
        element.acceptChildren(this, data)
    }

    override fun visitFile(declaration: IrFile, data: GlobalCompilationContext) {
        val library = data.info.component.createLibrary(data.names.nameFor(declaration))
        library.sourceUris.add(data.info.loadFile(declaration))

        declaration.acceptChildren(LibraryCompiler, data.inLibrary(library))
    }
}