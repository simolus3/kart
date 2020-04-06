package eu.simonbinder.kart.kotlin

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class DartBackendContext(
    module: ModuleDescriptor,
    override val irBuiltIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    irModuleFragment: IrModuleFragment,
    override val configuration: CompilerConfiguration
) : CommonBackendContext {

    override var inVerbosePhase = false

    override val internalPackageFqn: FqName
        get() = DART_PACKAGE_FQN

    override val sharedVariablesManager = DartSharedVariablesManager
    override val builtIns = module.builtIns
    override val declarationFactory = DartDeclarationFactory(this)

    val changedUnqualifiedNames = HashMap<IrDeclaration, String>()

    private val coroutinePackage = module.getPackage(COROUTINE_PACKAGE_FQNAME)
    private val coroutineContextProperty: PropertyDescriptor
        get() {
            val vars = coroutinePackage.memberScope.getContributedVariables(
                COROUTINE_CONTEXT_NAME,
                NoLookupLocation.FROM_BACKEND
            )
            return vars.single()
        }

    override val ir = object : Ir<DartBackendContext>(this, irModuleFragment) {
        override val symbols = object : Symbols<DartBackendContext>(this@DartBackendContext, symbolTable.lazyWrapper) {
            override val ThrowNoWhenBranchMatchedException: IrFunctionSymbol
                get() = TODO("not implemented")
            override val ThrowNullPointerException: IrFunctionSymbol
                get() = TODO("not implemented")
            override val ThrowTypeCastException: IrFunctionSymbol
                get() = TODO("not implemented")
            override val ThrowUninitializedPropertyAccessException: IrSimpleFunctionSymbol
                get() = TODO("not implemented")
            override val copyRangeTo: Map<ClassDescriptor, IrSimpleFunctionSymbol>
                get() = TODO("not implemented")
            override val coroutineContextGetter: IrSimpleFunctionSymbol
                get() = symbolTable.referenceSimpleFunction(context.coroutineContextProperty.getter!!)
            override val coroutineGetContext: IrSimpleFunctionSymbol
                get() = TODO("not implemented")
            override val coroutineImpl: IrClassSymbol
                get() = TODO("not implemented")
            override val coroutineSuspendedGetter: IrSimpleFunctionSymbol
                get() = TODO("not implemented")
            override val defaultConstructorMarker: IrClassSymbol
                get() = irBuiltIns.anyClass
            override val getContinuation: IrSimpleFunctionSymbol
                get() = TODO("not implemented")
            override val returnIfSuspended: IrSimpleFunctionSymbol
                get() = TODO("not implemented")
            override val stringBuilder: IrClassSymbol
                get() = TODO("not implemented")
            override val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol
                get() = TODO("not implemented")
        }
    }

    override fun log(message: () -> String) {
        if (inVerbosePhase) print(message())
    }

    override fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean) {
        print(message)
    }

    companion object {
        private val KOTLIN_PACKAGE_FQN = FqName.fromSegments(listOf("kotlin"))
        private val DART_PACKAGE_FQN = KOTLIN_PACKAGE_FQN.child(Name.identifier("dart"))

        private val COROUTINE_CONTEXT_NAME = Name.identifier("coroutineContext")
        private val COROUTINE_IMPL_NAME = Name.identifier("CoroutineImpl")
        private val CONTINUATION_NAME = Name.identifier("Continuation")

        private val COROUTINE_PACKAGE_FQNAME = FqName.fromSegments(listOf("kotlin", "coroutines"))
    }
}