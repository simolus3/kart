package eu.simonbinder.kart.transformer.compilers

import eu.simonbinder.kart.kernel.ast.FunctionNode
import eu.simonbinder.kart.kernel.ast.members.Field
import eu.simonbinder.kart.kernel.ast.members.Procedure
import eu.simonbinder.kart.kernel.ast.members.ProcedureKind
import eu.simonbinder.kart.kernel.ast.statements.VariableDeclaration
import eu.simonbinder.kart.kotlin.isStaticInDart
import eu.simonbinder.kart.transformer.context.InBodyCompilationContext
import eu.simonbinder.kart.transformer.context.MemberCompilationContext
import eu.simonbinder.kart.transformer.context.names
import eu.simonbinder.kart.transformer.identifierOrNull
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
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

    private val IrDeclaration.isStatic: Boolean get() = this@BaseMemberCompiler.isStatic || origin.isStaticInDart()

    override fun visitFunction(declaration: IrFunction, data: T) {
        if (declaration.origin == IrDeclarationOrigin.FAKE_OVERRIDE) return

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
        return compileFunctionNodeWithContextAndParameters(declaration, contextForBody, parameters, overrideBody)
    }

    protected fun compileFunctionNodeWithContextAndParameters(
        declaration: IrFunction,
        contextForBody: InBodyCompilationContext,
        params: List<VariableDeclaration>,
        overrideBody: IrBody? = declaration.body
    ): FunctionNode {
        return FunctionNode(
            body = overrideBody?.accept(BodyCompiler, contextForBody),
            positionalParameters = params,
            returnType = contextForBody.info.dartTypeFor(declaration.returnType)
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
        procedure.isStatic = declaration.isStatic
        procedure.isAbstract = declaration.body == null

        data.info.dartIntrinsics.applyIntrinsicProcedureName(procedure, declaration)

        return procedure
    }

    override fun visitProperty(declaration: IrProperty, data: T) {
        throw AssertionError("Property should have been lowered")
    }

    override fun visitField(declaration: IrField, data: T) {
        if (declaration.origin == IrDeclarationOrigin.FAKE_OVERRIDE) return

        // In Kernel, fields are closely linked to their corresponding getters and setters.
        val field = Field(
            name = data.names.simpleNameFor(declaration),
            reference = data.names.nameFor(declaration),
            type = data.info.dartTypeFor(declaration.type),
            initializer = declaration.initializer?.expression?.accept(ExpressionCompiler, data.toBody()),
            fileUri = data.info.loadFile(declaration.file)
        )

        // Not making non-static fields final because they're commonly accessed in the constructor body.
        val isFinal = isStatic && declaration.isFinal
        field.isFinal = isFinal
        field.isStatic = isStatic

        // we can't access non-static fields at all if they don't have an implicit getter / setter. We're generating a
        // proper getter / setter from Kotlin, but those have a different name.
        field.hasImplicitSetter = !isStatic && !isFinal
        field.hasImplicitGetter = !isStatic

        data.target.members.add(field)
    }

}