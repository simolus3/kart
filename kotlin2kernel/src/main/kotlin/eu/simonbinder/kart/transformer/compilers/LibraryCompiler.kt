package eu.simonbinder.kart.transformer.compilers

import eu.simonbinder.kart.kernel.FunctionNode
import eu.simonbinder.kart.kernel.Name
import eu.simonbinder.kart.kernel.members.Field
import eu.simonbinder.kart.kernel.members.Procedure
import eu.simonbinder.kart.kernel.members.ProcedureKind
import eu.simonbinder.kart.kernel.statements.VariableDeclaration
import eu.simonbinder.kart.transformer.context.InBodyCompilationContext
import eu.simonbinder.kart.transformer.context.InLibraryContext
import eu.simonbinder.kart.transformer.context.names
import eu.simonbinder.kart.transformer.identifierOrNull
import org.jetbrains.kotlin.backend.jvm.lower.constantValue
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.hasDefaultValue
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.util.isSetter
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

object LibraryCompiler : IrElementVisitor<Unit, InLibraryContext> {
    override fun visitElement(element: IrElement, data: InLibraryContext) {

    }

    private fun InLibraryContext.toBody() = InBodyCompilationContext(this)

    override fun visitFunction(declaration: IrFunction, data: InLibraryContext) {
        val dartReference = data.names.nameFor(declaration)

        val contextForBody = data.toBody()

        val parameters = declaration.valueParameters
            .sortedBy(IrValueParameter::index)
            .map {
                val dartVariable = VariableDeclaration(
                    name = it.name.identifierOrNull,
                    type = data.info.dartIntrinsics.intrinsicType(it.type)
                )
                dartVariable.isFinal = true
                contextForBody.variables[it.symbol] = dartVariable
                dartVariable
            }

        val kind = when {
            declaration.isGetter -> ProcedureKind.GETTER
            declaration.isSetter -> ProcedureKind.SETTER
            else -> ProcedureKind.METHOD
        }

        val procedure = Procedure(
            kind = kind,
            function = FunctionNode(
                body = declaration.body!!.accept(BodyCompiler, contextForBody),
                positionalParameters = parameters,
                returnType = data.info.dartIntrinsics.intrinsicType(declaration.returnType)
            ).also {
                it.fileOffset = declaration.startOffset
                it.endOffset = declaration.endOffset
            },
            name = data.info.names.simpleNameFor(declaration),
            reference = dartReference
        )
        procedure.fileUri = data.info.loadFile(declaration.file)
        procedure.startFileOffset = declaration.startOffset
        procedure.fileOffset = declaration.startOffset
        procedure.fileEndOffset = declaration.endOffset

        if (declaration.isMain()) {
            data.info.component.mainMethod = procedure
        }

        data.library.members.add(procedure)
    }

    override fun visitProperty(declaration: IrProperty, data: InLibraryContext) {
        declaration.acceptChildren(this, data)
    }

    override fun visitField(declaration: IrField, data: InLibraryContext) {
        // In Kernel, fields are closely linked to their corresponding getters and setters.
        val field = Field(
            name = data.names.simpleNameFor(declaration),
            reference = data.names.nameFor(declaration),
            type = data.info.dartTypeFor(declaration.type),
            initializer = declaration.initializer?.expression?.accept(ExpressionCompiler, data.toBody()),
            fileUri = data.info.loadFile(declaration.file)
        )

        field.isFinal = declaration.isFinal
        field.isStatic = true

        data.library.members.add(field)
    }

    private fun IrFunction.isMain(): Boolean {
        return name.identifierOrNull == "main" &&
                dispatchReceiverParameter == null &&
                extensionReceiverParameter == null &&
                valueParameters.isEmpty()
    }
}