package eu.simonbinder.kart.transformer.compilers

import eu.simonbinder.kart.kernel.members.Class
import eu.simonbinder.kart.kernel.types.DartType
import eu.simonbinder.kart.transformer.context.InClassContext
import eu.simonbinder.kart.transformer.context.InLibraryContext
import eu.simonbinder.kart.transformer.context.names
import eu.simonbinder.kart.transformer.identifierOrNull
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.isInterface

object LibraryCompiler : BaseMemberCompiler<InLibraryContext>() {

    override val isStatic: Boolean
        get() = true

    override fun visitClass(declaration: IrClass, data: InLibraryContext) {
        var superClass: DartType? = null
        val superInterfaces = mutableListOf<DartType>()

        for (superType in declaration.superTypes) {
            val isInterface = superType.classOrNull?.owner?.isInterface ?: false
            val dartType = data.info.dartTypeFor(superType)

            if (isInterface) {
                superInterfaces += dartType
            } else {
                assert(superClass == null)
                superClass = dartType
            }
        }

        assert(superClass != null) { "Kotlin class $declaration did not have a superclass" }

        val dartClass = Class(
            reference = data.names.nameFor(declaration),
            name = declaration.name.identifier,
            fileUri = data.info.loadFile(declaration.file),
            superClass = superClass,
            implementedClasses = superInterfaces
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
        val procedure = compileProcedure(declaration, data)
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