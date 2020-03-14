package eu.simonbinder.kart.transformer.compilers

import eu.simonbinder.kart.kernel.FunctionNode
import eu.simonbinder.kart.kernel.members.Field
import eu.simonbinder.kart.kernel.members.Procedure
import eu.simonbinder.kart.kernel.members.ProcedureKind
import eu.simonbinder.kart.kernel.statements.VariableDeclaration
import eu.simonbinder.kart.transformer.context.InBodyCompilationContext
import eu.simonbinder.kart.transformer.context.MemberCompilationContext
import eu.simonbinder.kart.transformer.context.names
import eu.simonbinder.kart.transformer.identifierOrNull
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.util.isSetter
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class BaseMemberCompiler<T: MemberCompilationContext> : IrElementVisitor<Unit, T> {

    override fun visitElement(element: IrElement, data: T) {

    }

    private fun MemberCompilationContext.toBody() = InBodyCompilationContext(this)

    protected abstract val isStatic: Boolean

    override fun visitFunction(declaration: IrFunction, data: T) {
        val procedure = compileProcedure(declaration, data)
        data.target.members.add(procedure)
    }

    protected fun createBodyContextAndParams(
        declaration: IrFunction,
        data: T
    ): Pair<InBodyCompilationContext, List<VariableDeclaration>> {
        val contextForBody = data.toBody()

        val parameters = declaration.valueParameters
            .sortedBy(IrValueParameter::index)
            .map {
                val dartVariable = VariableDeclaration(
                    name = it.name.identifierOrNull,
                    type = data.info.dartTypeFor(it.type)
                )
                dartVariable.isFinal = true
                contextForBody.variables[it.symbol] = dartVariable
                dartVariable
            }

        return contextForBody to parameters
    }

    protected fun compileFunctionNode(
        declaration: IrFunction,
        data: T,
        overrideBody: IrBody? = declaration.body
    ): FunctionNode {
        val (contextForBody, parameters) = createBodyContextAndParams(declaration, data)

        return FunctionNode(
            body = overrideBody?.accept(BodyCompiler, contextForBody),
            positionalParameters = parameters,
            returnType = data.info.dartTypeFor(declaration.returnType)
        ).also {
            it.fileOffset = declaration.startOffset
            it.endOffset = declaration.endOffset
        }
    }

    protected fun compileProcedure(declaration: IrFunction, data: T): Procedure {
        val dartReference = data.names.nameFor(declaration)
        val function = compileFunctionNode(declaration, data)

        val kind = when {
            declaration.isGetter -> ProcedureKind.GETTER
            declaration.isSetter -> ProcedureKind.SETTER
            else -> ProcedureKind.METHOD
        }

        val procedure = Procedure(
            kind = kind,
            function = function,
            name = data.info.names.simpleNameFor(declaration),
            reference = dartReference
        )
        procedure.fileUri = data.info.loadFile(declaration.file)
        procedure.startFileOffset = declaration.startOffset
        procedure.fileOffset = declaration.startOffset
        procedure.fileEndOffset = declaration.endOffset

        return procedure
    }

    override fun visitProperty(declaration: IrProperty, data: T) {
        throw AssertionError("Property should have been lowered")
    }

    override fun visitField(declaration: IrField, data: T) {
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

        data.target.members.add(field)
    }

}