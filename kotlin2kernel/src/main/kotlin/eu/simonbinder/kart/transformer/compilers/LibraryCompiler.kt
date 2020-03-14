package eu.simonbinder.kart.transformer.compilers

import eu.simonbinder.kart.kernel.members.Class
import eu.simonbinder.kart.transformer.context.InClassContext
import eu.simonbinder.kart.transformer.context.InLibraryContext
import eu.simonbinder.kart.transformer.context.names
import eu.simonbinder.kart.transformer.identifierOrNull
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.file

object LibraryCompiler : BaseMemberCompiler<InLibraryContext>() {

    override val isStatic: Boolean
        get() = true

    override fun visitClass(declaration: IrClass, data: InLibraryContext) {
        val dartClass = Class(
            reference = data.names.nameFor(declaration),
            name = declaration.name.identifier,
            fileUri = data.info.loadFile(declaration.file)
        )

        dartClass.isAbstract = declaration.modality == Modality.ABSTRACT
        dartClass.fileOffset = declaration.startOffset
        dartClass.startFileOffset = declaration.startOffset
        dartClass.fileEndOffset = declaration.endOffset

        val contextForChildren = InClassContext(data, dartClass)
        declaration.acceptChildren(ClassMemberCompiler, contextForChildren)

        data.library.classes.add(dartClass)
    }

    override fun visitFunction(declaration: IrFunction, data: InLibraryContext) {
        val procedure = compileFunction(declaration, data)
        data.target.members.add(procedure)

        if (declaration.isMain()) {
            data.info.component.mainMethod = procedure
        }
    }

    private fun IrFunction.isMain(): Boolean {
        return name.identifierOrNull == "main" &&
                dispatchReceiverParameter == null &&
                extensionReceiverParameter == null &&
                valueParameters.isEmpty()
    }
}