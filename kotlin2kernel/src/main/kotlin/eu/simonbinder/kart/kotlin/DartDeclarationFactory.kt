package eu.simonbinder.kart.kotlin

import org.jetbrains.kotlin.backend.common.ir.DeclarationFactory
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.types.IrType

class DartDeclarationFactory() : DeclarationFactory {
    override fun getFieldForEnumEntry(enumEntry: IrEnumEntry, entryType: IrType): IrField {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getFieldForObjectInstance(singleton: IrClass): IrField {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getInnerClassConstructorWithOuterThisParameter(innerClassConstructor: IrConstructor): IrConstructor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOuterThisField(innerClass: IrClass): IrField {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}