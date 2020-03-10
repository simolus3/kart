package eu.simonbinder.kart.transformer.compilers

import eu.simonbinder.kart.kernel.FunctionNode
import eu.simonbinder.kart.kernel.Name
import eu.simonbinder.kart.kernel.members.Procedure
import eu.simonbinder.kart.kernel.members.ProcedureKind
import eu.simonbinder.kart.kernel.statements.VariableDeclaration
import eu.simonbinder.kart.transformer.context.InBodyCompilationContext
import eu.simonbinder.kart.transformer.context.InLibraryContext
import eu.simonbinder.kart.transformer.context.names
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.hasDefaultValue
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

object LibraryCompiler : IrElementVisitor<Unit, InLibraryContext> {
    override fun visitElement(element: IrElement, data: InLibraryContext) {

    }

    override fun visitFunction(declaration: IrFunction, data: InLibraryContext) {
        val dartReference = data.names.nameFor(declaration)

        val contextForBody = InBodyCompilationContext(data)

        val parameters = declaration.valueParameters
            .sortedBy(IrValueParameter::index)
            .map {
                val dartVariable = VariableDeclaration(
                    name = it.name.identifier,
                    type = data.info.dartIntrinsics.intrinsicType(it.type)
                )
                dartVariable.isFinal = true
                contextForBody.variables[it.symbol] = dartVariable
                dartVariable
            }

        val procedure = Procedure(
            kind = ProcedureKind.METHOD,
            function = FunctionNode(
                body = declaration.body!!.accept(BodyCompiler, contextForBody),
                positionalParameters = parameters,
                returnType = data.info.dartIntrinsics.intrinsicType(declaration.returnType)
            ).also {
                it.fileOffset = declaration.startOffset
                it.endOffset = declaration.endOffset
            },
            name = Name(declaration.name.identifier),
            reference = dartReference
        )
        procedure.fileUri = data.info.loadFile(declaration.file)
        procedure.startFileOffset = declaration.startOffset

        if (declaration.isMain()) {
            data.info.component.mainMethod = procedure
        }

        data.library.members.add(procedure)
    }

    private fun IrFunction.isMain(): Boolean {
        return name.identifier == "main" &&
                dispatchReceiverParameter == null &&
                extensionReceiverParameter == null &&
                valueParameters.isEmpty()
    }
}