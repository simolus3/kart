package eu.simonbinder.kart.kotlin

import eu.simonbinder.kart.kotlin.lower.*
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.inline.FunctionInlining
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.ir.backend.js.lower.inline.RemoveInlineFunctionsWithReifiedTypeParametersLowering
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.patchDeclarationParents

private fun makeCustomIrModulePhase(
    op: (CommonBackendContext, IrModuleFragment) -> Unit,
    description: String,
    name: String,
    prerequisite: Set<AnyNamedPhase> = emptySet()
) = namedIrModulePhase(
    name,
    description,
    prerequisite,
    actions = setOf(defaultDumper),
    nlevels = 0,
    lower = object : SameTypeCompilerPhase<CommonBackendContext, IrModuleFragment> {
        override fun invoke(
            phaseConfig: PhaseConfig,
            phaserState: PhaserState<IrModuleFragment>,
            context: CommonBackendContext,
            input: IrModuleFragment
        ): IrModuleFragment {
            op(context, input)
            return input
        }
    }
)

private val expectDeclarationsRemovingPhase = makeIrModulePhase(
    ::ExpectDeclarationsRemoveLowering,
    name = "ExpectDeclarationsRemoveLowering",
    description = "Remove expect declaration from module fragment"
)

private val stripTypeAliasDeclarationsPhase = makeIrModulePhase<CommonBackendContext>(
    { StripTypeAliasDeclarationsLowering() },
    name = "StripTypeAliasDeclarations",
    description = "Strip typealias declarations"
)

private val innerClassesLoweringPhase = makeIrModulePhase(
    ::InnerClassesLowering,
    name = "InnerClassesLowering",
    description = "Capture outer this reference to inner class"
)

private val innerClassConstructorCallsLoweringPhase = makeIrModulePhase(
    ::InnerClassConstructorCallsLowering,
    name = "InnerClassConstructorCallsLowering",
    description = "Replace inner class constructor invocation"
)

private val propertiesLoweringPhase = makeIrModulePhase<DartBackendContext>(
    { context -> PropertiesLowering(context) },
    name = "PropertiesLowering",
    description = "Move fields and accessors out from its property"
)

private val initializersLoweringPhase = makeIrModulePhase(
    { context: DartBackendContext -> InitializersLowering(context, DartDeclarationOrigin.LOWERED_INIT_BLOCK, false) },
    name = "Initializers",
    description = "Lower initializers"
)

private val cleanRemovedInitializersLowering = makeIrModulePhase<DartBackendContext>(
    { CleanRemovedInitializersLowering() },
    name = "CleanRemovedInitializersLowering",
    description = "Remove initializers that were transformed to the constructor"
)

private val defaultArgumentStubGeneratorPhase = makeIrModulePhase(
    ::DartDefaultArgumentStubGenerator,
    name = "DefaultArgumentStubGenerator",
    description = "Generate synthetic stubs for functions with default parameter values"
)

private val defaultParameterInjectorPhase = makeIrModulePhase(
    { context: CommonBackendContext -> DefaultParameterInjector(context, skipExternalMethods = true) },
    name = "DefaultParameterInjector",
    description = "Replace callsize with default parameters with corresponding stub function"
)

private val defaultParameterCleanerPhase = makeIrModulePhase(
    ::DefaultParameterCleaner,
    name = "DefaultParameterCleaner",
    description = "Remove default parameters from functions"
)

private val functionInliningPhase = makeCustomIrModulePhase(
    { context, module ->
        FunctionInlining(context).inline(module)
        module.patchDeclarationParents()
    },
    name = "FunctionInliningPhase",
    description = "Perform function inlining",
    prerequisite = setOf(expectDeclarationsRemovingPhase)
)

private val tailrecLoweringPhase = makeIrModulePhase(
    ::TailrecLowering,
    name = "TailrecLowering",
    description = "Replace `tailrec` callsites with equivalent loop"
)

private val removeInlineFunctionsWithReifiedTypeParametersLoweringPhase = makeIrModulePhase<CommonBackendContext>(
    { RemoveInlineFunctionsWithReifiedTypeParametersLowering() },
    name = "RemoveInlineFunctionsWithReifiedTypeParametersLowering",
    description = "Remove Inline functions with reified parameters from context",
    prerequisite = setOf(functionInliningPhase)
)

private val expressionToStatementLoweringPhase = makeIrModulePhase(
    ::ExpressionToStatementLowering,
    name = "ExpressionToStatementLoweringPhase",
    description = "Transforms complex expressions into statements by introducing variables"
)

private val primitiveTypeLoweringPhase = makeIrModulePhase(
    ::PrimitiveTypeLowering,
    name = "PrimitiveTypeLowering",
    description = "Replace usages of primitive types with Long and Double"
)

private val typeOperatorLowering = makeIrModulePhase(
    ::TypeOperatorLowering,
    name = "TypeOperatorLowering",
    description = "Replace some common type operators in IR"
)

val dartPhases = namedIrModulePhase(
    name = "DartIrModuleLowering",
    description = "Lower Kotlin IR to make Dart compilation easier",
    lower = expectDeclarationsRemovingPhase then
            stripTypeAliasDeclarationsPhase then
            innerClassesLoweringPhase then
            innerClassConstructorCallsLoweringPhase then
            propertiesLoweringPhase then
            initializersLoweringPhase then
            cleanRemovedInitializersLowering then
            defaultArgumentStubGeneratorPhase then
            defaultParameterInjectorPhase then
            defaultParameterCleanerPhase then
            // functionInliningPhase then
            //removeInlineFunctionsWithReifiedTypeParametersLoweringPhase then
            tailrecLoweringPhase then
            expressionToStatementLoweringPhase then
            primitiveTypeLoweringPhase then
            typeOperatorLowering
)