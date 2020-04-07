package eu.simonbinder.kart.kotlin

import org.jetbrains.kotlin.backend.common.descriptors.WrappedClassConstructorDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.backend.common.ir.DeclarationFactory
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createStaticFunctionWithReceivers
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

class DartDeclarationFactory(private val context: DartBackendContext) : DeclarationFactory {

    private val outerThisDeclarations = HashMap<IrClass, IrField>()
    private val innerClassConstructors = HashMap<IrConstructor, IrConstructor>()

    private val singletonsToInstanceField = HashMap<IrClass, IrField>()

    private val defaultImplsMethods = HashMap<IrSimpleFunction, IrSimpleFunction>()

    override fun getFieldForEnumEntry(enumEntry: IrEnumEntry, entryType: IrType): IrField {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getFieldForObjectInstance(singleton: IrClass): IrField {
        return singletonsToInstanceField.getOrPut(singleton) {
            // If the class was a nested / inner / companion class that was raised to the library level, it will have a
            // changed unqualified name that we should respect for uniques purposes. Otherwise, two different nested
            // classes with the same (unqualified) name would have the same instance field name.
            val safeName = context.changedUnqualifiedNames[singleton] ?: singleton.name.identifier

            buildField {
                name = Name.identifier(safeName + "_instance")
                origin = IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE
                type = singleton.defaultType
                isStatic = true
                isFinal = true
            }.apply {
                // Kotlin IR doesn't support static fields (only companion objects), so put this field in the enclosing
                // library
                parent = singleton.parent
            }
        }
    }

    override fun getInnerClassConstructorWithOuterThisParameter(innerClassConstructor: IrConstructor): IrConstructor {
        return innerClassConstructors.getOrPut(innerClassConstructor) {
            createInnerClassConstructorWithOuterThisParameter(innerClassConstructor)
        }
    }

    override fun getOuterThisField(innerClass: IrClass): IrField {
        // reference to the outer class for inner classes. Replaces this@OuterClass
        return outerThisDeclarations.getOrPut(innerClass) {
            buildField {
                name = Name.identifier("this\$outer")
                type = innerClass.parentAsClass.defaultType
                origin = DeclarationFactory.FIELD_FOR_OUTER_THIS
                isFinal = true
            }.apply {
                parent = innerClass
            }
        }
    }

    private fun createInnerClassConstructorWithOuterThisParameter(oldConstructor: IrConstructor): IrConstructor {
        val newDescriptor = WrappedClassConstructorDescriptor(oldConstructor.descriptor.annotations)
        return IrConstructorImpl(
            oldConstructor.startOffset, oldConstructor.endOffset, oldConstructor.origin,
            IrConstructorSymbolImpl(newDescriptor),
            oldConstructor.name, oldConstructor.visibility, oldConstructor.returnType,
            oldConstructor.isInline, oldConstructor.isExternal, oldConstructor.isPrimary
        ).apply {
            newDescriptor.bind(this)
            annotations.addAll(oldConstructor.annotations.map { it.deepCopyWithSymbols(this) })
            parent = oldConstructor.parent
            returnType = oldConstructor.returnType
            copyTypeParametersFrom(oldConstructor)

            val outerThisType = oldConstructor.parentAsClass.parentAsClass.defaultType
            val outerThisDescriptor = WrappedValueParameterDescriptor()
            val outerThisValueParameter = IrValueParameterImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, JvmLoweredDeclarationOrigin.FIELD_FOR_OUTER_THIS,
                IrValueParameterSymbolImpl(outerThisDescriptor),
                Name.identifier("\$outer"),
                0,
                type = outerThisType,
                varargElementType = null,
                isCrossinline = false,
                isNoinline = false
            ).also {
                outerThisDescriptor.bind(it)
                it.parent = this
            }
            valueParameters.add(outerThisValueParameter)

            oldConstructor.valueParameters.mapTo(valueParameters) { it.copyTo(this, index = it.index + 1) }
            metadata = oldConstructor.metadata
        }
    }

    fun staticDefaultImplForInterface(oldFunction: IrSimpleFunction): IrSimpleFunction {
        val parentInterface = oldFunction.parentAsClass
        assert(parentInterface.isInterface)

        return defaultImplsMethods.getOrPut(oldFunction) {
            val name = Name.identifier(oldFunction.name.identifier + "\$defaultImpl")

            createStaticFunctionWithReceivers(
                parentInterface,
                name,
                oldFunction,
                dispatchReceiverType = parentInterface.defaultType,
                origin = DartFunctionOrigin.StaticDefaultMethod
            )
        }
    }

}