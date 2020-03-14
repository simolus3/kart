package eu.simonbinder.kart.transformer.names

import eu.simonbinder.kart.kernel.CanonicalName
import eu.simonbinder.kart.kernel.Name as DartName
import eu.simonbinder.kart.kernel.Reference
import eu.simonbinder.kart.kernel.asReference
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

class Names {

    val root = CanonicalName.root()
    val dartNames = ImportantDartNames(root)

    private val fileToName: MutableMap<IrFile, Reference> = mutableMapOf()
    private val declarationToName: MutableMap<IrDeclaration, Reference> = mutableMapOf()

    fun nameFor(declaration: IrDeclaration): Reference = declarationToName.computeIfAbsent(declaration) {
        // todo support class members
        val library = nameFor(declaration.file)
        val baseName = library.canonicalName!!

        val name = if (declaration is IrField) {
            // In Kernel, Fields and getters/setters are weirdly mixed up (one can use a StaticGet that refers to a
            // getter, for instance). To cleanly separate fields and their accessors, we always suffix the field.
            baseName.getChild(CanonicalName.FIELDS).getChild(declaration.name.identifier + "_field")
        } else if (declaration is IrSimpleFunction) {
            if (declaration.isGetter || declaration.isSetter) {
                val nameOfField = declaration.correspondingPropertySymbol!!.descriptor.name.identifier
                baseName
                    .getChild(if (declaration.isGetter) CanonicalName.GETTERS else CanonicalName.SETTERS)
                    .getChild(nameOfField)
            } else {
                baseName.getChild(CanonicalName.METHODS).getChild(declaration.name.identifier)
            }
        } else if (declaration is IrClass) {
            baseName.getChild(declaration.name.identifier)
        } else {
            TODO()
        }

        name.asReference()
    }

    fun simpleNameFor(declaration: IrDeclaration, includeField: Boolean = true): DartName {
        return if (declaration.isGetter || declaration.isSetter) {
            simpleNameFor((declaration as IrSimpleFunction).correspondingPropertySymbol!!.owner, false)
        } else if (includeField && declaration is IrField) {
            DartName(declaration.name.identifier + "_field")
        } else {
            DartName(declaration.nameForIrSerialization.identifier)
        }
    }

    fun nameFor(file: IrFile): Reference = fileToName.computeIfAbsent(file) {
        val fqn = file.fqName
        val name = root.getSubChild(if (fqn.isRoot) listOf("rootKt") else fqn.pathSegments().map(Name::getIdentifier))
        Reference(name)
    }
}