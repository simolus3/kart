package eu.simonbinder.kart.kotlin.lower.interfaces

import eu.simonbinder.kart.kotlin.DartBackendContext
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.DescriptorsToIrRemapper
import org.jetbrains.kotlin.backend.common.ir.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.backend.common.ir.passTypeArgumentsFrom
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.*

/**
 * For classes that implement interfaces with default methods, adds a bridge method to that static implementation.
 *
 * For instance, let's say we had the following interface with a lowered default method and a class implementing that
 * interface.
 *
 * ```kotlin
 * interface Foo {
 *   fun m1(): Int
 *   fun m2():Int = /*2 * m1()*/ // body removed during DefaultImplementationsLowering
 *
 *   /*static*/ fun m2$defaultImpl(`this`: Foo) = 2 * `this`.m1()
 * }
 *
 * class Bar : Foo {
 *   override fun m1() = 1
 * }
 * ```
 *
 * We would now add an implementation for the `m2` method in `Bar`:
 * ```kotlin
 * class Bar : Foo {
 *   override fun m1() = 1
 *   override fun m2() = Foo.m2$defaultImpl(this)
 * }
 * ```
 */
class InterfaceDelegationLowering(val context: DartBackendContext): ClassLoweringPass {

    override fun lower(irClass: IrClass) {
        if (irClass.isInterface) return

        generateInterfaceMethods(irClass)
        // todo adapt overriddenSymbols like the jvm lowering does?
    }

    private fun generateInterfaceMethods(irClass: IrClass) {
        for (function in irClass.functions.toList()) { // copy because we'll modify the list here
            if (function.origin != IrDeclarationOrigin.FAKE_OVERRIDE) continue

            val implementation = function.resolveFakeOverride() ?: continue
            if (!implementation.parentAsClass.isInterface) continue

            val newOverride = createDelegationFunction(irClass, implementation, function)

            irClass.declarations.add(newOverride)
            irClass.declarations.remove(function)
        }
    }

    private fun createDelegationFunction(
        irClass: IrClass,
        interfaceFun: IrSimpleFunction,
        fakeOverride: IrSimpleFunction
    ): IrSimpleFunction {
        val defaultImpl = context.declarationFactory.staticDefaultImplForInterface(interfaceFun)

        // Copied from InterfaceDelegationLowering in the JVM backend
        val inheritedProperty = fakeOverride.correspondingPropertySymbol?.owner
        val descriptor = DescriptorsToIrRemapper.remapDeclaredSimpleFunction(fakeOverride.descriptor)
        val newOverride = IrFunctionImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            IrSimpleFunctionSymbolImpl(descriptor),
            fakeOverride.name,
            Visibilities.PUBLIC,
            fakeOverride.modality,
            fakeOverride.returnType,
            isInline = fakeOverride.isInline,
            isExternal = false,
            isTailrec = false,
            isSuspend = fakeOverride.isSuspend
        ).apply {
            descriptor.bind(this)
            parent = irClass
            overriddenSymbols.addAll(fakeOverride.overriddenSymbols)
            copyParameterDeclarationsFrom(fakeOverride)
            annotations.addAll(fakeOverride.annotations)

            if (inheritedProperty != null) {
                val propertyDescriptor = DescriptorsToIrRemapper.remapDeclaredProperty(inheritedProperty.descriptor)
                IrPropertyImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.DEFINED, IrPropertySymbolImpl(propertyDescriptor),
                    inheritedProperty.name, Visibilities.PUBLIC, inheritedProperty.modality, inheritedProperty.isVar,
                    inheritedProperty.isConst, inheritedProperty.isLateinit, inheritedProperty.isDelegated, isExternal = false
                ).apply {
                    propertyDescriptor.bind(this)
                    parent = irClass
                    correspondingPropertySymbol = symbol
                }
            }
        }

        context.createIrBuilder(newOverride.symbol).apply {
            newOverride.body = irBlockBody {
                +irReturn(
                    irCall(defaultImpl.symbol, newOverride.returnType).apply {
                        passTypeArgumentsFrom(newOverride)
                        var offset = 0

                        newOverride.dispatchReceiverParameter?.let { putValueArgument(offset++, irGet(it)) }
                        newOverride.extensionReceiverParameter?.let { putValueArgument(offset++, irGet(it)) }
                        newOverride.valueParameters.forEachIndexed { i, parameter ->
                            putValueArgument(offset + i, irGet(parameter))
                        }
                    }
                )
            }
        }

        return newOverride
    }

}