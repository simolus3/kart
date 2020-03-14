package eu.simonbinder.kart.transformer.invoke

import eu.simonbinder.kart.kernel.Reference
import eu.simonbinder.kart.kernel.Name as DartName
import eu.simonbinder.kart.kernel.expressions.*
import eu.simonbinder.kart.kernel.types.*
import eu.simonbinder.kart.transformer.names.ImportantDartNames
import eu.simonbinder.kart.transformer.withIrOffsets
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class DartIntrinsics (
    private val dartNames: ImportantDartNames,
    private val irBuiltIns: IrBuiltIns,
    private val symbolTable: SymbolTable
) {

    private val kotlinIntTypes = listOf(
        irBuiltIns.byteType,
        irBuiltIns.charType,
        irBuiltIns.shortType,
        irBuiltIns.intType,
        irBuiltIns.longType
    )

    fun intrinsicCall(call: IrCall, exprCompiler: (IrExpression) -> Expression): Expression? {
        fun getCompiled(index: Int): Expression {
            return exprCompiler(call.getValueArgument(index)!!)
        }

        if (call.dispatchReceiver?.type in kotlinIntTypes) {
            // todo: This logic should be implemented in PrimitiveTypeLowering. We should also add an appropriate
            // bitmask.
            lateinit var dartName: DartName
            lateinit var reference: Reference

            when (call.symbol.descriptor.name.identifier) {
                "plus" -> {
                    dartName = DartName("+")
                    reference = dartNames.numPlus
                }
                "minus" -> {
                    dartName = DartName("-")
                    reference = dartNames.numMinus
                }
                "times" -> {
                    dartName = DartName("*")
                    reference = dartNames.numTimes
                }
                "div" -> {
                    dartName = DartName("~/")
                    reference = dartNames.numTruncatingDivision
                }
                "mod" -> {
                    dartName = DartName("%")
                    reference = dartNames.numMod
                }
                "and" -> {
                    dartName = DartName("&")
                    reference = dartNames.intAnd
                }
            }

            return MethodInvocation(
                receiver = exprCompiler(call.dispatchReceiver!!),
                name = dartName,
                reference = reference,
                arguments = Arguments(
                    positional = listOf(getCompiled(0))
                )
            )
        }

        return when (val symbol = call.symbol) {
            // a === b, use identical(a, b) in Dart
            irBuiltIns.eqeqeqSymbol -> StaticInvocation(
                dartNames.identical,
                Arguments(
                    positional = listOf(
                        getCompiled(0),
                        getCompiled(1)
                    )
                )
            )
            // a == b, use a == b in Dart, which is a method invocation on a
            irBuiltIns.eqeqSymbol -> MethodInvocation(
                receiver = getCompiled(0),
                name = DartName("==", null),
                reference = dartNames.objectEquals,
                arguments = Arguments(
                    positional = listOf(getCompiled(1))
                )
            )
            // a || b or a && b, compile to LogicalExpression in Kernel (which respects short-circuits)
            irBuiltIns.ororSymbol, irBuiltIns.andandSymbol -> {
                LogicalExpression(
                    left = getCompiled(0),
                    right = getCompiled(1),
                    operator = if (symbol == irBuiltIns.ororSymbol) LogicalOperator.OR else LogicalOperator.AND
                )
            }
            irBuiltIns.booleanNotSymbol -> Not(exprCompiler(call.dispatchReceiver!!))
            irBuiltIns.checkNotNullSymbol -> NullCheck(getCompiled(0)).withIrOffsets(call)
            in irBuiltIns.lessFunByOperandType.values -> {
                MethodInvocation(
                    receiver = getCompiled(0),
                    name = DartName("<", null),
                    reference = dartNames.numLess,
                    arguments = Arguments(positional = listOf(getCompiled(1)))
                )
            }
            else -> {
                println("Call not known as intrinsic: ${symbol.descriptor.fqNameSafe}")
                null
            }
        }
    }

    fun intrinsicType(type: IrType): DartType {
        if (type is IrDynamicType) return DynamicType

        return when(type.classOrNull) {
            irBuiltIns.byteClass,
            irBuiltIns.charClass,
            irBuiltIns.shortClass,
            irBuiltIns.intClass,
            irBuiltIns.longClass -> dartNames.intType.withNullabilityOfIr(type)
            irBuiltIns.floatClass,
            irBuiltIns.doubleClass -> dartNames.doubleType.withNullabilityOfIr(type)
            irBuiltIns.unitClass -> VoidType
            irBuiltIns.stringClass -> dartNames.stringType.withNullabilityOfIr(type)
            irBuiltIns.nothingClass -> NeverType(if (type.isNullable()) Nullability.NULLABLE else Nullability.NON_NULLABLE)
            irBuiltIns.anyClass, irBuiltIns.throwableClass -> {
                if (type.isNullable()) {
                    DynamicType // todo: Should Any? be mapped to void in Dart? Or to dart:core::Object?
                } else {
                    dartNames.objectType
                }
            }
            else -> throw IllegalArgumentException("Cannot map ${type.classOrNull?.descriptor?.fqNameOrNull() ?: type} to Dart")
        }
    }

    private fun DartType.withNullabilityOfIr(type: IrType): DartType {
        if (this !is InterfaceType) return this

        val nullable = (type as? IrSimpleType)?.hasQuestionMark ?: false
        return withNullability(if (nullable) Nullability.NULLABLE else Nullability.NON_NULLABLE)
    }
}